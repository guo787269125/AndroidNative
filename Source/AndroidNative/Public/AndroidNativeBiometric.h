// Georgy Treshchev 2024.

#pragma once

#include "CoreMinimal.h"
#include "Kismet/BlueprintAsyncActionBase.h"
#include "AndroidNativeBiometric.generated.h"

/**
 * Fires when a biometric authentication attempt finishes. Error is empty on success.
 */
DECLARE_DYNAMIC_MULTICAST_DELEGATE_TwoParams(FAndroidNativeBiometricResult, bool, bSuccess, const FString&, Error);

/**
 * Blueprint async node: prompt the user for biometric (fingerprint/face) authentication.
 *
 * NOTE: androidx BiometricPrompt requires a FragmentActivity; stock Unreal GameActivity
 * usually isn't one, so this may report an error until GameActivity is adapted. Requires
 * an on-device Unreal build (with the androidx.biometric dependency) to compile and verify.
 */
UCLASS()
class ANDROIDNATIVE_API UAuthenticateBiometricProxy : public UBlueprintAsyncActionBase
{
	GENERATED_BODY()

public:
	UPROPERTY(BlueprintAssignable)
	FAndroidNativeBiometricResult OnResult;

	UFUNCTION(BlueprintCallable, meta = (BlueprintInternalUseOnly = "true"), Category = "Android Native Library|Biometric")
	static UAuthenticateBiometricProxy* AuthenticateBiometric(const FString& Title, const FString& Subtitle, const FString& CancelText);

	virtual void Activate() override;

private:
	FString Title;
	FString Subtitle;
	FString CancelText;
	int32 RequestId = 0;
	FDelegateHandle ResultHandle;

	void HandleResult(int32 InRequestId, bool bSuccess, const FString& Error);
};
