// Georgy Treshchev 2024.

#pragma once

#include "CoreMinimal.h"
#include "Kismet/BlueprintAsyncActionBase.h"
#include "AndroidNativeReview.generated.h"

/**
 * Fires when the in-app review flow finishes. bSuccess reflects that the flow completed
 * (Play never reveals whether the user actually left a review).
 */
DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(FAndroidNativeReviewResult, bool, bSuccess);

/**
 * Blueprint async node: request the Google Play in-app review flow.
 * Requires the com.google.android.play:review dependency (added by the UPL) and an
 * on-device Unreal build to compile and verify.
 */
UCLASS()
class ANDROIDNATIVE_API URequestInAppReviewProxy : public UBlueprintAsyncActionBase
{
	GENERATED_BODY()

public:
	UPROPERTY(BlueprintAssignable)
	FAndroidNativeReviewResult OnResult;

	UFUNCTION(BlueprintCallable, meta = (BlueprintInternalUseOnly = "true"), Category = "Android Native Library|Review")
	static URequestInAppReviewProxy* RequestInAppReview();

	virtual void Activate() override;

private:
	int32 RequestId = 0;
	FDelegateHandle ResultHandle;

	void HandleResult(int32 InRequestId, bool bSuccess);
};
