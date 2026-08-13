// Georgy Treshchev 2024.

#include "AndroidNativePermissions.h"

#include "AndroidNativeDefines.h"
#include "JavaConvert.h"
#include "Async/Async.h"

#if PLATFORM_ANDROID
#include "Android/AndroidApplication.h"
#include "Android/AndroidJNI.h"
#endif

// Native multicast that the JNI callback broadcasts into. Active request proxies subscribe to it.
DECLARE_MULTICAST_DELEGATE_TwoParams(FAndroidNativeOnPermissionResultNative, const TArray<FString>&, const TArray<bool>&);
static FAndroidNativeOnPermissionResultNative GAndroidNativeOnPermissionResult;

URequestAndroidPermissionsProxy* URequestAndroidPermissionsProxy::RequestAndroidPermissions(const TArray<FString>& Permissions)
{
	URequestAndroidPermissionsProxy* Proxy = NewObject<URequestAndroidPermissionsProxy>();
	Proxy->RequestedPermissions = Permissions;
	return Proxy;
}

void URequestAndroidPermissionsProxy::Activate()
{
	ResultHandle = GAndroidNativeOnPermissionResult.AddUObject(this, &URequestAndroidPermissionsProxy::HandleNativeResult);

#if PLATFORM_ANDROID
	if (JNIEnv* Env = FAndroidApplication::GetJavaEnv())
	{
		jobjectArray JavaPermissions = AndroidNative_JavaConverter::ToJavaStringArray(RequestedPermissions);

		// Call the GameActivity instance method injected by the UPL additions.
		static jmethodID RequestMethod = FJavaWrapper::FindMethod(Env, FJavaWrapper::GameActivityClassID,
			"AndroidNative_RequestPermissions", "([Ljava/lang/String;)V", false);

		if (RequestMethod != nullptr)
		{
			FJavaWrapper::CallVoidMethod(Env, FJavaWrapper::GameActivityThis, RequestMethod, JavaPermissions);
		}

		if (JavaPermissions != nullptr)
		{
			Env->DeleteLocalRef(JavaPermissions);
		}
		return;
	}

	UE_LOG(LogAndroidNative, Error, TEXT("RequestAndroidPermissions: failed to get Java environment"));
#else
	// Off-device: report everything as not granted so the graph still completes.
	TArray<bool> Granted;
	Granted.Init(false, RequestedPermissions.Num());
	HandleNativeResult(RequestedPermissions, Granted);
#endif
}

void URequestAndroidPermissionsProxy::HandleNativeResult(const TArray<FString>& Permissions, const TArray<bool>& GrantResults)
{
	GAndroidNativeOnPermissionResult.Remove(ResultHandle);
	ResultHandle.Reset();

	OnResult.Broadcast(Permissions, GrantResults);
	SetReadyToDestroy();
}

#if PLATFORM_ANDROID
// Implements the `public native void nativeAndroidNativeOnPermissionResult(String[], boolean[])`
// declared on GameActivity by the UPL additions.
//
// IMPORTANT: the mangled name below must match your engine's GameActivity package.
//   - UE4 / early UE5 : Java_com_epicgames_ue4_GameActivity_...
//   - UE5 (5.0+)      : Java_com_epicgames_unreal_GameActivity_...
// This plugin's UPL references com.epicgames.ue4.* elsewhere, so the ue4 name is used here.
extern "C" JNIEXPORT void JNICALL Java_com_epicgames_ue4_GameActivity_nativeAndroidNativeOnPermissionResult(
	JNIEnv* Env, jobject /*Thiz*/, jobjectArray JavaPermissions, jbooleanArray JavaGranted)
{
	TArray<FString> Permissions = AndroidNative_JavaConverter::FromJavaStringArray(JavaPermissions);
	TArray<bool> Granted = AndroidNative_JavaConverter::FromJavaBoolArray(JavaGranted);

	// The JNI callback may arrive on a non-game thread; marshal the broadcast to the game thread.
	AsyncTask(ENamedThreads::GameThread, [Permissions, Granted]()
	{
		GAndroidNativeOnPermissionResult.Broadcast(Permissions, Granted);
	});
}
#endif
