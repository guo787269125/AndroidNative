// Georgy Treshchev 2024.

#include "AndroidNativeMedia.h"

#include "AndroidNativeDefines.h"
#include "AndroidNativeUtils.h"
#include "JavaConvert.h"
#include "Async/Async.h"

#if PLATFORM_ANDROID
#include "Android/AndroidApplication.h"
#include "Android/AndroidJNI.h"
#endif

static const ANSICHAR* AN_MediaDeviceInfoClassName = "com/Plugins/AndroidNative/DeviceInfo";

// Android Activity.RESULT_OK
static const int32 AN_RESULT_OK = -1;

// Native multicast broadcast by the activity-result JNI callback; proxies filter by request code.
DECLARE_MULTICAST_DELEGATE_ThreeParams(FAndroidNativeOnActivityResultNative, int32, int32, const FString&);
static FAndroidNativeOnActivityResultNative GAndroidNativeOnActivityResult;

static int32 GAndroidNativeNextRequestCode = 20000;
static int32 AN_AllocateRequestCode()
{
	return GAndroidNativeNextRequestCode++;
}

// ---- PickImage ----

UAndroidNativePickImageProxy* UAndroidNativePickImageProxy::PickImageFromGallery()
{
	return NewObject<UAndroidNativePickImageProxy>();
}

void UAndroidNativePickImageProxy::Activate()
{
	RequestCode = AN_AllocateRequestCode();
	ResultHandle = GAndroidNativeOnActivityResult.AddUObject(this, &UAndroidNativePickImageProxy::HandleActivityResult);
	AndroidNativeUtils::CallJavaStaticMethod<void>(AN_MediaDeviceInfoClassName, "PickImageFromGallery", FAndroidGameActivity(), RequestCode);

#if !PLATFORM_ANDROID
	HandleActivityResult(RequestCode, 0, FString());
#endif
}

void UAndroidNativePickImageProxy::HandleActivityResult(int32 InRequestCode, int32 ResultCode, const FString& Uri)
{
	if (InRequestCode != RequestCode)
	{
		return;
	}

	GAndroidNativeOnActivityResult.Remove(ResultHandle);
	ResultHandle.Reset();

	const bool bSuccess = (ResultCode == AN_RESULT_OK) && !Uri.IsEmpty();
	OnResult.Broadcast(bSuccess, Uri);
	SetReadyToDestroy();
}

// ---- PickFile ----

UAndroidNativePickFileProxy* UAndroidNativePickFileProxy::PickFile(const FString& InMimeType)
{
	UAndroidNativePickFileProxy* Proxy = NewObject<UAndroidNativePickFileProxy>();
	Proxy->MimeType = InMimeType;
	return Proxy;
}

void UAndroidNativePickFileProxy::Activate()
{
	RequestCode = AN_AllocateRequestCode();
	ResultHandle = GAndroidNativeOnActivityResult.AddUObject(this, &UAndroidNativePickFileProxy::HandleActivityResult);
	AndroidNativeUtils::CallJavaStaticMethod<void>(AN_MediaDeviceInfoClassName, "PickFile", FAndroidGameActivity(), RequestCode, MimeType);

#if !PLATFORM_ANDROID
	HandleActivityResult(RequestCode, 0, FString());
#endif
}

void UAndroidNativePickFileProxy::HandleActivityResult(int32 InRequestCode, int32 ResultCode, const FString& Uri)
{
	if (InRequestCode != RequestCode)
	{
		return;
	}

	GAndroidNativeOnActivityResult.Remove(ResultHandle);
	ResultHandle.Reset();

	const bool bSuccess = (ResultCode == AN_RESULT_OK) && !Uri.IsEmpty();
	OnResult.Broadcast(bSuccess, Uri);
	SetReadyToDestroy();
}

#if PLATFORM_ANDROID
// Implements DeviceInfo.nativeOnActivityResult(int, int, String). Static native -> jclass.
extern "C" JNIEXPORT void JNICALL Java_com_Plugins_AndroidNative_DeviceInfo_nativeOnActivityResult(
	JNIEnv* /*Env*/, jclass /*Clazz*/, jint RequestCode, jint ResultCode, jstring JavaUri)
{
	const FString Uri = AndroidNative_JavaConverter::FromJavaString(JavaUri);
	const int32 Req = static_cast<int32>(RequestCode);
	const int32 Res = static_cast<int32>(ResultCode);

	AsyncTask(ENamedThreads::GameThread, [Req, Res, Uri]()
	{
		GAndroidNativeOnActivityResult.Broadcast(Req, Res, Uri);
	});
}
#endif
