// Georgy Treshchev 2024.

#include "AndroidNativeLocation.h"

#include "AndroidNativeDefines.h"
#include "AndroidNativeUtils.h"
#include "Async/Async.h"

#if PLATFORM_ANDROID
#include "Android/AndroidApplication.h"
#include "Android/AndroidJNI.h"
#endif

static const ANSICHAR* AN_LocationDeviceInfoClassName = "com/Plugins/AndroidNative/DeviceInfo";

DECLARE_MULTICAST_DELEGATE_FourParams(FAndroidNativeOnLocationResultNative, int32, bool, double, double);
static FAndroidNativeOnLocationResultNative GAndroidNativeOnLocationResult;

static int32 GAndroidNativeNextLocationId = 30000;

URequestAndroidLocationProxy* URequestAndroidLocationProxy::RequestSingleLocation()
{
	return NewObject<URequestAndroidLocationProxy>();
}

void URequestAndroidLocationProxy::Activate()
{
	RequestId = GAndroidNativeNextLocationId++;
	ResultHandle = GAndroidNativeOnLocationResult.AddUObject(this, &URequestAndroidLocationProxy::HandleResult);
	AndroidNativeUtils::CallJavaStaticMethod<void>(AN_LocationDeviceInfoClassName, "RequestSingleLocation", FAndroidGameActivity(), RequestId);

#if !PLATFORM_ANDROID
	HandleResult(RequestId, false, 0.0, 0.0);
#endif
}

void URequestAndroidLocationProxy::HandleResult(int32 InRequestId, bool bSuccess, double Latitude, double Longitude)
{
	if (InRequestId != RequestId)
	{
		return;
	}

	GAndroidNativeOnLocationResult.Remove(ResultHandle);
	ResultHandle.Reset();

	OnResult.Broadcast(bSuccess, static_cast<float>(Latitude), static_cast<float>(Longitude));
	SetReadyToDestroy();
}

#if PLATFORM_ANDROID
extern "C" JNIEXPORT void JNICALL Java_com_Plugins_AndroidNative_DeviceInfo_nativeOnLocationResult(
	JNIEnv* /*Env*/, jclass /*Clazz*/, jint RequestId, jboolean Success, jdouble Latitude, jdouble Longitude)
{
	const int32 Req = static_cast<int32>(RequestId);
	const bool bSuccess = (Success == JNI_TRUE);
	const double Lat = static_cast<double>(Latitude);
	const double Lng = static_cast<double>(Longitude);

	AsyncTask(ENamedThreads::GameThread, [Req, bSuccess, Lat, Lng]()
	{
		GAndroidNativeOnLocationResult.Broadcast(Req, bSuccess, Lat, Lng);
	});
}
#endif
