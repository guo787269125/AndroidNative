// Georgy Treshchev 2024.

#pragma once

#include "CoreMinimal.h"
#include "Kismet/BlueprintAsyncActionBase.h"
#include "AndroidNativePermissions.generated.h"

/**
 * Fires when the user responds to a runtime permission request.
 * Permissions[i] corresponds to GrantResults[i] (true = granted).
 */
DECLARE_DYNAMIC_MULTICAST_DELEGATE_TwoParams(FAndroidNativePermissionResult, const TArray<FString>&, Permissions, const TArray<bool>&, GrantResults);

/**
 * Blueprint async node that requests one or more Android runtime permissions and
 * fires OnResult when the user responds (or immediately if already granted).
 *
 * Implementation note: the result is delivered from Unreal's GameActivity via a
 * native JNI callback (see AndroidNativePermissions.cpp + the UPL GameActivity
 * additions). This requires an on-device Unreal build to compile and verify.
 */
UCLASS()
class ANDROIDNATIVE_API URequestAndroidPermissionsProxy : public UBlueprintAsyncActionBase
{
	GENERATED_BODY()

public:
	UPROPERTY(BlueprintAssignable)
	FAndroidNativePermissionResult OnResult;

	/**
	 * Request runtime permissions (e.g. "android.permission.CAMERA"). Any already-granted
	 * permissions are reported as granted without prompting.
	 */
	UFUNCTION(BlueprintCallable, meta = (BlueprintInternalUseOnly = "true"), Category = "Android Native Library|Permissions")
	static URequestAndroidPermissionsProxy* RequestAndroidPermissions(const TArray<FString>& Permissions);

	virtual void Activate() override;

private:
	UPROPERTY()
	TArray<FString> RequestedPermissions;

	FDelegateHandle ResultHandle;

	void HandleNativeResult(const TArray<FString>& Permissions, const TArray<bool>& GrantResults);
};
