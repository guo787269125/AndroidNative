// Georgy Treshchev 2024.

#include "AndroidNativeLibrary.h"
#include "AndroidNative.h"

#include "AndroidNativeUtils.h"

static const ANSICHAR* DeviceInfoClassName = "com/Plugins/AndroidNative/DeviceInfo";

void UAndroidNativeLibrary::EnableNsdService(int32 InPort)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "startNsdService", FAndroidGameActivity(), InPort);
}

void UAndroidNativeLibrary::DisableNsdService()
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "StopNsdService");
}

bool UAndroidNativeLibrary::IsInternetAvailable()
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "IsInternetAvailable", FAndroidGameActivity());
}

FString UAndroidNativeLibrary::GetGeoLocation()
{
	return AndroidNativeUtils::CallJavaStaticMethod<FString>(DeviceInfoClassName, "GetGeoLocation", FAndroidGameActivity());
}

FBaseDeviceInfo UAndroidNativeLibrary::GetBaseDeviceInfo()
{
	// Fetch everything in a single JNI round-trip (the Java side fills the array).
	const TArray<FString> Info{AndroidNativeUtils::CallJavaStaticMethod<TArray<FString>>(DeviceInfoClassName, "GetBaseDeviceInfo", FAndroidGameActivity())};

	FBaseDeviceInfo Result;
	if (Info.Num() >= 8)
	{
		Result.UniqueID = Info[0];
		Result.OSVersion = Info[1];
		Result.SDKVersion = FCString::Atoi(*Info[2]);
		Result.Brand = Info[3];
		Result.Model = Info[4];
		Result.Product = Info[5];
		Result.Language = Info[6];
		Result.LanguageCode = Info[7];
	}

	return Result;
}

EAndroidTheme UAndroidNativeLibrary::GetCurrentSystemTheme()
{
	const uint8 Theme{AndroidNativeUtils::CallJavaStaticMethod<uint8>(DeviceInfoClassName, "GetCurrentSystemTheme", FAndroidGameActivity())};

	switch (Theme)
	{
	case 0: return EAndroidTheme::Undefined;
	case 1: return EAndroidTheme::Light;
	case 2: return EAndroidTheme::Dark;
	default: return EAndroidTheme::Undefined;
	}
}

FString UAndroidNativeLibrary::GetExternalPath()
{
	return AndroidNativeUtils::CallJavaStaticMethod<FString>(DeviceInfoClassName, "GetExternalPath", FAndroidGameActivity());
}

void UAndroidNativeLibrary::CopyTextToClipboard(const FString& InText)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "CopyToClipboard", FAndroidGameActivity(), InText);
}

bool UAndroidNativeLibrary::IsTorchAvailable()
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "IsTorchAvailable", FAndroidGameActivity());
}

bool UAndroidNativeLibrary::SetTorchEnabled(bool bEnabled)
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "SetTorchEnabled", FAndroidGameActivity(), bEnabled);
}

bool UAndroidNativeLibrary::OpenGallery()
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "OpenGallery", FAndroidGameActivity());
}

bool UAndroidNativeLibrary::MakePhoneCall(const FString& PhoneNumber)
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "MakePhoneCall", FAndroidGameActivity(), PhoneNumber);
}

bool UAndroidNativeLibrary::SendEmail(const FString& Recipient, const FString& Subject, const FString& Body)
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "SendEmail", FAndroidGameActivity(), Recipient, Subject, Body);
}

void UAndroidNativeLibrary::Vibrate(int32 DurationMilliseconds)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "Vibrate", FAndroidGameActivity(), DurationMilliseconds);
}

void UAndroidNativeLibrary::ShowToast(const FString& Message, bool bLongDuration)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "ShowToast", FAndroidGameActivity(), Message, bLongDuration);
}

bool UAndroidNativeLibrary::ShareText(const FString& Text, const FString& Title)
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "ShareText", FAndroidGameActivity(), Text, Title);
}

int32 UAndroidNativeLibrary::GetBatteryLevel()
{
	return AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetBatteryLevel", FAndroidGameActivity());
}

bool UAndroidNativeLibrary::IsCharging()
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "IsCharging", FAndroidGameActivity());
}

void UAndroidNativeLibrary::SetKeepScreenOn(bool bKeepOn)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "SetKeepScreenOn", FAndroidGameActivity(), bKeepOn);
}

bool UAndroidNativeLibrary::OpenURL(const FString& Url)
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "OpenURL", FAndroidGameActivity(), Url);
}

bool UAndroidNativeLibrary::OpenAppSettings()
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "OpenAppSettings", FAndroidGameActivity());
}

FString UAndroidNativeLibrary::GetAppVersion()
{
	return AndroidNativeUtils::CallJavaStaticMethod<FString>(DeviceInfoClassName, "GetAppVersion", FAndroidGameActivity());
}

int32 UAndroidNativeLibrary::GetAppVersionCode()
{
	return AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetAppVersionCode", FAndroidGameActivity());
}

FString UAndroidNativeLibrary::GetPackageName()
{
	return AndroidNativeUtils::CallJavaStaticMethod<FString>(DeviceInfoClassName, "GetPackageName", FAndroidGameActivity());
}
