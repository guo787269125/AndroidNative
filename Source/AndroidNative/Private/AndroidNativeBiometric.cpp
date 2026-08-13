// Georgy Treshchev 2024.

#include "AndroidNativeBiometric.h"

#include "AndroidNativeDefines.h"
#include "AndroidNativeUtils.h"
#include "JavaConvert.h"
#include "Async/Async.h"

#if PLATFORM_ANDROID
#include "Android/AndroidApplication.h"
#include "Android/AndroidJNI.h"
#endif

static const ANSICHAR* AN_BiometricClassName = "com/Plugins/AndroidNative/BiometricAuth";

DECLARE_MULTICAST_DELEGATE_ThreeParams(FAndroidNativeOnBiometricResultNative, int32, bool, const FString&);
static FAndroidNativeOnBiometricResultNative GAndroidNativeOnBiometricResult;

static int32 GAndroidNativeNextBiometricId = 40000;

UAuthenticateBiometricProxy* UAuthenticateBiometricProxy::AuthenticateBiometric(const FString& InTitle, const FString& InSubtitle, const FString& InCancelText)
{
	UAuthenticateBiometricProxy* Proxy = NewObject<UAuthenticateBiometricProxy>();
	Proxy->Title = InTitle;
	Proxy->Subtitle = InSubtitle;
	Proxy->CancelText = InCancelText;
	return Proxy;
}

void UAuthenticateBiometricProxy::Activate()
{
	RequestId = GAndroidNativeNextBiometricId++;
	ResultHandle = GAndroidNativeOnBiometricResult.AddUObject(this, &UAuthenticateBiometricProxy::HandleResult);
	AndroidNativeUtils::CallJavaStaticMethod<void>(AN_BiometricClassName, "Authenticate", FAndroidGameActivity(), RequestId, Title, Subtitle, CancelText);

#if !PLATFORM_ANDROID
	HandleResult(RequestId, false, TEXT("Not supported on this platform"));
#endif
}

void UAuthenticateBiometricProxy::HandleResult(int32 InRequestId, bool bSuccess, const FString& Error)
{
	if (InRequestId != RequestId)
	{
		return;
	}

	GAndroidNativeOnBiometricResult.Remove(ResultHandle);
	ResultHandle.Reset();

	OnResult.Broadcast(bSuccess, Error);
	SetReadyToDestroy();
}

#if PLATFORM_ANDROID
extern "C" JNIEXPORT void JNICALL Java_com_Plugins_AndroidNative_BiometricAuth_nativeOnBiometricResult(
	JNIEnv* /*Env*/, jclass /*Clazz*/, jint RequestId, jboolean Success, jstring JavaError)
{
	const int32 Req = static_cast<int32>(RequestId);
	const bool bSuccess = (Success == JNI_TRUE);
	const FString Error = AndroidNative_JavaConverter::FromJavaString(JavaError);

	AsyncTask(ENamedThreads::GameThread, [Req, bSuccess, Error]()
	{
		GAndroidNativeOnBiometricResult.Broadcast(Req, bSuccess, Error);
	});
}
#endif
