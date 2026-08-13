// Georgy Treshchev 2024.

#pragma once

#include "CoreMinimal.h"
#include "Kismet/BlueprintAsyncActionBase.h"
#include "AndroidNativeMedia.generated.h"

/**
 * Fires when a picker activity returns. bSuccess is true when the user selected an item;
 * Uri is the content:// URI string of the selection (empty on cancel/failure).
 */
DECLARE_DYNAMIC_MULTICAST_DELEGATE_TwoParams(FAndroidNativePickResult, bool, bSuccess, const FString&, Uri);

/**
 * Blueprint async node: open the gallery image picker and return the selected image URI.
 * Result is delivered via GameActivity.onActivityResult -> DeviceInfo.nativeOnActivityResult.
 * Requires an on-device Unreal build to compile and verify.
 */
UCLASS()
class ANDROIDNATIVE_API UAndroidNativePickImageProxy : public UBlueprintAsyncActionBase
{
	GENERATED_BODY()

public:
	UPROPERTY(BlueprintAssignable)
	FAndroidNativePickResult OnResult;

	UFUNCTION(BlueprintCallable, meta = (BlueprintInternalUseOnly = "true"), Category = "Android Native Library|Media")
	static UAndroidNativePickImageProxy* PickImageFromGallery();

	virtual void Activate() override;

private:
	int32 RequestCode = 0;
	FDelegateHandle ResultHandle;

	void HandleActivityResult(int32 InRequestCode, int32 ResultCode, const FString& Uri);
};

/**
 * Blueprint async node: open a file picker (ACTION_GET_CONTENT) with the given MIME type
 * (empty = any) and return the selected URI.
 */
UCLASS()
class ANDROIDNATIVE_API UAndroidNativePickFileProxy : public UBlueprintAsyncActionBase
{
	GENERATED_BODY()

public:
	UPROPERTY(BlueprintAssignable)
	FAndroidNativePickResult OnResult;

	UFUNCTION(BlueprintCallable, meta = (BlueprintInternalUseOnly = "true"), Category = "Android Native Library|Media")
	static UAndroidNativePickFileProxy* PickFile(const FString& MimeType);

	virtual void Activate() override;

private:
	FString MimeType;
	int32 RequestCode = 0;
	FDelegateHandle ResultHandle;

	void HandleActivityResult(int32 InRequestCode, int32 ResultCode, const FString& Uri);
};
