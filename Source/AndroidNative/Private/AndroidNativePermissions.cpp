// Georgy Treshchev 2024.

#include "AndroidNativePermissions.h"

#include "AndroidNativeDefines.h"
#include "AndroidNativeUtils.h"
#include "JavaConvert.h"
#include "Async/Async.h"

#if PLATFORM_ANDROID
#include "Android/AndroidApplication.h"
#include "Android/AndroidJNI.h"
#endif

static const ANSICHAR* AN_DeviceInfoClassName = "com/Plugins/AndroidNative/DeviceInfo";

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

	// DeviceInfo.RequestPermissions(Activity, String[]) triggers the request; the result
	// is delivered back through nativeOnPermissionResult (see below + the UPL additions).
	AndroidNativeUtils::CallJavaStaticMethod<void>(AN_DeviceInfoClassName, "RequestPermissions", FAndroidGameActivity(), RequestedPermissions);

#if !PLATFORM_ANDROID
	// Off-device: complete the graph immediately, reporting nothing granted.
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
// Implements DeviceInfo.nativeOnPermissionResult(String[], boolean[]). Static native -> jclass.
extern "C" JNIEXPORT void JNICALL Java_com_Plugins_AndroidNative_DeviceInfo_nativeOnPermissionResult(
	JNIEnv* /*Env*/, jclass /*Clazz*/, jobjectArray JavaPermissions, jbooleanArray JavaGranted)
{
	TArray<FString> Permissions = AndroidNative_JavaConverter::FromJavaStringArray(JavaPermissions);
	TArray<bool> Granted = AndroidNative_JavaConverter::FromJavaBoolArray(JavaGranted);

	AsyncTask(ENamedThreads::GameThread, [Permissions, Granted]()
	{
		GAndroidNativeOnPermissionResult.Broadcast(Permissions, Granted);
	});
}
#endif
