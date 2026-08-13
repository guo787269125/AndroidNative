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

void UAndroidNativeLibrary::SetScreenBrightness(int32 Percent)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "SetScreenBrightness", FAndroidGameActivity(), Percent);
}

int32 UAndroidNativeLibrary::GetScreenBrightness()
{
	return AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetScreenBrightness", FAndroidGameActivity());
}

void UAndroidNativeLibrary::SetScreenOrientation(EScreenOrientation Orientation)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "SetScreenOrientation", FAndroidGameActivity(), static_cast<int32>(Orientation));
}

void UAndroidNativeLibrary::SetMusicVolume(int32 Percent)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "SetMusicVolume", FAndroidGameActivity(), Percent);
}

int32 UAndroidNativeLibrary::GetMusicVolume()
{
	return AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetMusicVolume", FAndroidGameActivity());
}

EAndroidNetworkType UAndroidNativeLibrary::GetNetworkType()
{
	const int32 Type{AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetNetworkType", FAndroidGameActivity())};
	switch (Type)
	{
	case 1: return EAndroidNetworkType::WiFi;
	case 2: return EAndroidNetworkType::Cellular;
	case 3: return EAndroidNetworkType::Other;
	default: return EAndroidNetworkType::None;
	}
}

int32 UAndroidNativeLibrary::GetTotalMemoryMB()
{
	return AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetTotalMemoryMB", FAndroidGameActivity());
}

int32 UAndroidNativeLibrary::GetAvailableMemoryMB()
{
	return AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetAvailableMemoryMB", FAndroidGameActivity());
}

int32 UAndroidNativeLibrary::GetTotalStorageMB()
{
	return AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetTotalStorageMB", FAndroidGameActivity());
}

int32 UAndroidNativeLibrary::GetAvailableStorageMB()
{
	return AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetAvailableStorageMB", FAndroidGameActivity());
}

int32 UAndroidNativeLibrary::GetStatusBarHeight()
{
	return AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetStatusBarHeight", FAndroidGameActivity());
}

int32 UAndroidNativeLibrary::GetNavigationBarHeight()
{
	return AndroidNativeUtils::CallJavaStaticMethod<int32>(DeviceInfoClassName, "GetNavigationBarHeight", FAndroidGameActivity());
}

void UAndroidNativeLibrary::SetImmersiveMode(bool bEnabled)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "SetImmersiveMode", FAndroidGameActivity(), bEnabled);
}

bool UAndroidNativeLibrary::OpenMap(float Latitude, float Longitude, const FString& Label)
{
	// Build the geo: URI on the C++ side so no floating-point values cross the JNI boundary.
	FString GeoUri{FString::Printf(TEXT("geo:%f,%f"), Latitude, Longitude)};
	if (!Label.IsEmpty())
	{
		GeoUri += FString::Printf(TEXT("?q=%f,%f(%s)"), Latitude, Longitude, *Label);
	}
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "OpenGeoUri", FAndroidGameActivity(), GeoUri);
}

FString UAndroidNativeLibrary::GetClipboardText()
{
	return AndroidNativeUtils::CallJavaStaticMethod<FString>(DeviceInfoClassName, "GetClipboardText", FAndroidGameActivity());
}

bool UAndroidNativeLibrary::IsAppInstalled(const FString& PackageName)
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "IsAppInstalled", FAndroidGameActivity(), PackageName);
}

bool UAndroidNativeLibrary::OpenApp(const FString& PackageName)
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "OpenApp", FAndroidGameActivity(), PackageName);
}

bool UAndroidNativeLibrary::IsPermissionGranted(const FString& Permission)
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "IsPermissionGranted", FAndroidGameActivity(), Permission);
}

void UAndroidNativeLibrary::ShowNotification(const FString& Title, const FString& Text, int32 NotificationId)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(DeviceInfoClassName, "ShowNotification", FAndroidGameActivity(), Title, Text, NotificationId);
}

bool UAndroidNativeLibrary::AreNotificationsEnabled()
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(DeviceInfoClassName, "AreNotificationsEnabled", FAndroidGameActivity());
}
