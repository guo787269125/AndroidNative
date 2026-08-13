// Georgy Treshchev 2024.

#pragma once

#include "CoreMinimal.h"
#include "Kismet/BlueprintAsyncActionBase.h"
#include "AndroidNativeLocation.generated.h"

/**
 * Fires when a single-location request completes. Latitude/Longitude are valid only when bSuccess is true.
 * Note: exposed as float for Blueprint compatibility; expect ~metre-level precision loss vs the native double.
 */
DECLARE_DYNAMIC_MULTICAST_DELEGATE_ThreeParams(FAndroidNativeLocationResult, bool, bSuccess, float, Latitude, float, Longitude);

/**
 * Blueprint async node: request a single fresh location fix. Requires ACCESS_FINE_LOCATION
 * or ACCESS_COARSE_LOCATION (request it first via RequestAndroidPermissions).
 * Requires an on-device Unreal build to compile and verify.
 */
UCLASS()
class ANDROIDNATIVE_API URequestAndroidLocationProxy : public UBlueprintAsyncActionBase
{
	GENERATED_BODY()

public:
	UPROPERTY(BlueprintAssignable)
	FAndroidNativeLocationResult OnResult;

	UFUNCTION(BlueprintCallable, meta = (BlueprintInternalUseOnly = "true"), Category = "Android Native Library|Location")
	static URequestAndroidLocationProxy* RequestSingleLocation();

	virtual void Activate() override;

private:
	int32 RequestId = 0;
	FDelegateHandle ResultHandle;

	void HandleResult(int32 InRequestId, bool bSuccess, double Latitude, double Longitude);
};
