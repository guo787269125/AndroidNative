// Georgy Treshchev 2024.

#pragma once

#include "Kismet/BlueprintFunctionLibrary.h"
#include "AndroidNativeLibrary.generated.h"

/**
 * Basic information about the device
 */
USTRUCT(BlueprintType, Category = "Android Native Library|Stuctures")
struct FBaseDeviceInfo
{
	GENERATED_BODY()

	UPROPERTY(BlueprintReadWrite, Category = "Android Native Library|Stuctures")
	FString UniqueID;

	UPROPERTY(BlueprintReadWrite, Category = "Android Native Library|Stuctures")
	FString OSVersion;

	UPROPERTY(BlueprintReadWrite, Category = "Android Native Library|Stuctures")
	int32 SDKVersion = -1;

	UPROPERTY(BlueprintReadWrite, Category = "Android Native Library|Stuctures")
	FString Brand;

	UPROPERTY(BlueprintReadWrite, Category = "Android Native Library|Stuctures")
	FString Model;

	UPROPERTY(BlueprintReadWrite, Category = "Android Native Library|Stuctures")
	FString Product;

	UPROPERTY(BlueprintReadWrite, Category = "Android Native Library|Stuctures")
	FString Language;

	UPROPERTY(BlueprintReadWrite, Category = "Android Native Library|Stuctures")
	FString LanguageCode;
};

/**
 * Android themes
 */
UENUM(BlueprintType, Category = "Android Native Library|Enumerators")
enum class EAndroidTheme : uint8
{
	Undefined,
	Light,
	Dark
};

/**
 * Requested screen orientation
 */
UENUM(BlueprintType, Category = "Android Native Library|Enumerators")
enum class EScreenOrientation : uint8
{
	Unspecified,
	Portrait,
	Landscape,
	ReversePortrait,
	ReverseLandscape,
	Sensor
};

/**
 * Active network transport type
 */
UENUM(BlueprintType, Category = "Android Native Library|Enumerators")
enum class EAndroidNetworkType : uint8
{
	None,
	WiFi,
	Cellular,
	Other
};

/**
 * Library for interacting with android device
 */
UCLASS(BlueprintType, Category = "Android Native Library")
class ANDROIDNATIVE_API UAndroidNativeLibrary : public UBlueprintFunctionLibrary
{
	GENERATED_BODY()

public:
	
	/**
	 * Start NSD Service on defined port with "NdiNsdService" name and "_ndi._tcp." type.
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Services|NSD")
	static void EnableNsdService(int32 InPort);

	/**
	 * Stop a previously started NSD Service. Safe to call even if none is running.
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Services|NSD")
	static void DisableNsdService();
	
	/**
	 * Check if the internet is available at the moment
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Basic")
	static bool IsInternetAvailable();

	/**
	 * Retrieve the device's location, requires "android.permission.ACCESS_FINE_LOCATION" and "android.permission.INTERNET" permissions
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Basic")
	static FString GetGeoLocation();

	/**
	 * Obtain basic information about the device
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Basic")
	static FBaseDeviceInfo GetBaseDeviceInfo();

	/**
	 * Get the folder path for storing temporary files on the device ("storage/emulated/0/Android/data/data/%APP_PACKAGE_NAME%/")
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Basic")
	static FString GetExternalPath();

	/**
	 * Retrieve the current system theme used in the device
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Basic")
	static EAndroidTheme GetCurrentSystemTheme();

	/**
	 * Copy text to Android clipboard
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Basic")
	static void CopyTextToClipboard(const FString& InText);

	/**
	 * Returns true if the device has a camera flash that can be used as a torch (Android 6.0 / API 23+)
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Hardware")
	static bool IsTorchAvailable();

	/**
	 * Turn the device flashlight (torch) on or off. Returns true if the torch state was applied
	 * (requires Android 6.0 / API 23 or newer and a camera with a flash unit)
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Hardware")
	static bool SetTorchEnabled(bool bEnabled);

	/**
	 * Open the system gallery / photos app so the user can browse images
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Intents")
	static bool OpenGallery();

	/**
	 * Start a phone call to the given number. Requires the "android.permission.CALL_PHONE"
	 * runtime permission; if it is not granted, the dialer is opened pre-filled with the number instead
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Intents")
	static bool MakePhoneCall(const FString& PhoneNumber);

	/**
	 * Open the system email composer pre-filled with the given recipient, subject and body
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Intents")
	static bool SendEmail(const FString& Recipient, const FString& Subject, const FString& Body);

	/**
	 * Vibrate the device for the given duration. Requires the "android.permission.VIBRATE" permission
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Hardware")
	static void Vibrate(int32 DurationMilliseconds);

	/**
	 * Show a system Toast message (bLongDuration selects LENGTH_LONG vs LENGTH_SHORT)
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static void ShowToast(const FString& Message, bool bLongDuration);

	/**
	 * Open the system share sheet with the given plain text
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Intents")
	static bool ShareText(const FString& Text, const FString& Title);

	/**
	 * Battery level as a percentage in [0, 100], or -1 if unavailable
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static int32 GetBatteryLevel();

	/**
	 * True if the device is currently charging or already full
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static bool IsCharging();

	/**
	 * Keep the screen on while your app is in the foreground (or release the request)
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static void SetKeepScreenOn(bool bKeepOn);

	/**
	 * Open the given URL in the default browser / handler
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Intents")
	static bool OpenURL(const FString& Url);

	/**
	 * Open this application's system settings (details) page
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Intents")
	static bool OpenAppSettings();

	/**
	 * Application version name (e.g. "1.2.3"), or empty on failure
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static FString GetAppVersion();

	/**
	 * Application version code, or -1 on failure
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static int32 GetAppVersionCode();

	/**
	 * Application package name (e.g. "com.company.game")
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static FString GetPackageName();

	/**
	 * Set the app window brightness. Percent in [0,100], or a negative value to follow the system
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static void SetScreenBrightness(int32 Percent);

	/**
	 * Current app window brightness in [0,100], or -1 if it follows the system
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static int32 GetScreenBrightness();

	/**
	 * Lock/request the screen orientation
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static void SetScreenOrientation(EScreenOrientation Orientation);

	/**
	 * Set the music-stream volume. Percent in [0,100]
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static void SetMusicVolume(int32 Percent);

	/**
	 * Music-stream volume as a percentage in [0,100], or -1 on failure
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static int32 GetMusicVolume();

	/**
	 * Active network transport type
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static EAndroidNetworkType GetNetworkType();

	/**
	 * Total physical RAM in MB, or -1 on failure
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static int32 GetTotalMemoryMB();

	/**
	 * Available RAM in MB, or -1 on failure
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static int32 GetAvailableMemoryMB();

	/**
	 * Total internal storage in MB, or -1 on failure
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static int32 GetTotalStorageMB();

	/**
	 * Available internal storage in MB, or -1 on failure
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static int32 GetAvailableStorageMB();

	/**
	 * Status bar height in pixels (0 if unknown)
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static int32 GetStatusBarHeight();

	/**
	 * Navigation bar height in pixels (0 if unknown)
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static int32 GetNavigationBarHeight();

	/**
	 * Enable/disable sticky immersive fullscreen
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|System")
	static void SetImmersiveMode(bool bEnabled);

	/**
	 * Open a maps app at the given coordinates (Label is an optional marker title)
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Intents")
	static bool OpenMap(float Latitude, float Longitude, const FString& Label);

	/**
	 * Read the current clipboard text (empty if none)
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Basic")
	static FString GetClipboardText();

	/**
	 * True if the given package is installed (subject to Android 11+ package visibility)
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Intents")
	static bool IsAppInstalled(const FString& PackageName);

	/**
	 * Launch another installed app by package name (subject to Android 11+ package visibility)
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Intents")
	static bool OpenApp(const FString& PackageName);

	/**
	 * True if the given runtime permission (e.g. "android.permission.CAMERA") is currently granted.
	 * To request a permission at runtime, use the RequestAndroidPermissions async node.
	 */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Permissions")
	static bool IsPermissionGranted(const FString& Permission);
};