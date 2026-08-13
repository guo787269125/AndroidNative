// Georgy Treshchev 2024.

#include "AndroidNativeReview.h"

#include "AndroidNativeDefines.h"
#include "AndroidNativeUtils.h"
#include "Async/Async.h"

#if PLATFORM_ANDROID
#include "Android/AndroidApplication.h"
#include "Android/AndroidJNI.h"
#endif

static const ANSICHAR* AN_ReviewClassName = "com/Plugins/AndroidNative/ReviewHelper";

DECLARE_MULTICAST_DELEGATE_TwoParams(FAndroidNativeOnReviewCompleteNative, int32, bool);
static FAndroidNativeOnReviewCompleteNative GAndroidNativeOnReviewComplete;

static int32 GAndroidNativeNextReviewId = 50000;

URequestInAppReviewProxy* URequestInAppReviewProxy::RequestInAppReview()
{
	return NewObject<URequestInAppReviewProxy>();
}

void URequestInAppReviewProxy::Activate()
{
	RequestId = GAndroidNativeNextReviewId++;
	ResultHandle = GAndroidNativeOnReviewComplete.AddUObject(this, &URequestInAppReviewProxy::HandleResult);
	AndroidNativeUtils::CallJavaStaticMethod<void>(AN_ReviewClassName, "RequestReview", FAndroidGameActivity(), RequestId);

#if !PLATFORM_ANDROID
	HandleResult(RequestId, false);
#endif
}

void URequestInAppReviewProxy::HandleResult(int32 InRequestId, bool bSuccess)
{
	if (InRequestId != RequestId)
	{
		return;
	}

	GAndroidNativeOnReviewComplete.Remove(ResultHandle);
	ResultHandle.Reset();

	OnResult.Broadcast(bSuccess);
	SetReadyToDestroy();
}

#if PLATFORM_ANDROID
extern "C" JNIEXPORT void JNICALL Java_com_Plugins_AndroidNative_ReviewHelper_nativeOnReviewComplete(
	JNIEnv* /*Env*/, jclass /*Clazz*/, jint RequestId, jboolean Success)
{
	const int32 Req = static_cast<int32>(RequestId);
	const bool bSuccess = (Success == JNI_TRUE);

	AsyncTask(ENamedThreads::GameThread, [Req, bSuccess]()
	{
		GAndroidNativeOnReviewComplete.Broadcast(Req, bSuccess);
	});
}
#endif
