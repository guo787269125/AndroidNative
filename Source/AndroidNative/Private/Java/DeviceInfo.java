// Georgy Treshchev 2024.
// FF Reality

// Note: Save this file in UTF-8 without BOM format to ensure successful compilation

package com.Plugins.AndroidNative;

import androidx.annotation.Keep;
import android.app.Activity;

import android.os.Build;
import android.provider.Settings;
import android.app.Activity;
import android.content.Context;
import java.io.File;
import android.content.res.Resources;
import androidx.core.os.ConfigurationCompat;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.content.res.Configuration;

import android.content.ClipData;
import android.content.ClipboardManager;

// Connectivity
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

// Hardware / Intents
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;

// Batch 1: system utilities
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.VibrationEffect;
import android.os.BatteryManager;
import android.widget.Toast;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;

// Batch 2: system utilities
import android.media.AudioManager;
import android.app.ActivityManager;
import android.os.StatFs;
import android.os.Environment;
import android.content.pm.ActivityInfo;

// Permissions
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

// Sensors
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

// Notifications
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

// NSD Service
import android.os.IBinder;
import android.app.Service;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

@Keep
public class DeviceInfo {

	static NsdManager nsdManager;
	static NsdServiceInfo nsdServiceInfo;
	static NsdManager.RegistrationListener listener;
	static Context context;

	// Cached id of a camera that has a flash unit (resolved lazily, reused across torch calls)
	static String flashCameraId;
	static boolean flashCameraResolved;

	// Active sensor listeners keyed by Android sensor type
	static final java.util.HashMap<Integer, SensorEventListener> sensorListeners = new java.util.HashMap<Integer, SensorEventListener>();

	// Native callbacks implemented in C++ (AndroidNativeCallbacks.cpp). Routed through this
	// class so the JNI symbol names are engine-version independent.
	@Keep public static native void nativeOnPermissionResult(String[] permissions, boolean[] granted);
	@Keep public static native void nativeOnActivityResult(int requestCode, int resultCode, String dataUri);
	@Keep public static native void nativeOnLifecycle(int lifecycleEvent);
	@Keep public static native void nativeOnSensorChanged(int sensorType, float x, float y, float z);
	@Keep public static native void nativeOnLocationResult(int requestId, boolean success, double latitude, double longitude);

	@Keep
	public static void startNsdService(final Activity activity, int Port) {
		try {
			context = activity;

			// Tear down any previously registered service so repeated calls don't leak a listener.
			StopNsdService();

			nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
			if (nsdManager == null) {
				return;
			}

			nsdServiceInfo = new NsdServiceInfo();
			nsdServiceInfo.setServiceName("NdiNsdService");
			nsdServiceInfo.setServiceType("_ndi._tcp.");
			nsdServiceInfo.setPort(Port);

			// Keep a reference to the listener so the service can be unregistered later.
			listener = new NsdManager.RegistrationListener() {

				@Override
				public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {}

				@Override
				public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {}

				@Override
				public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {}

				@Override
				public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {}
			};

			nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, listener);
		} catch (Exception e) {
			// NSD registration is best-effort
		}
	}

	/**
	 * Stop a previously started NSD service. Safe to call even if nothing is registered.
	 */
	@Keep
	public static void StopNsdService() {
		try {
			if (nsdManager != null && listener != null) {
				nsdManager.unregisterService(listener);
			}
		} catch (Exception e) {
			// May already be unregistered
		} finally {
			listener = null;
		}
	}

    @Keep
    public static String GetGeoLocation(final Activity activity)
    {
        try {
            Context context = activity;
            LocationManager mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
            if (mLocationManager == null) {
                return "";
            }

            Location locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNet = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (locationGPS == null && locationNet == null) {
                return "";
            }
            if (locationGPS == null) {
                return locationNet.toString();
            }
            if (locationNet == null) {
                return locationGPS.toString();
            }

            if (locationGPS.getTime() - locationNet.getTime() > 0) {
                return locationGPS.toString();
            }
            return locationNet.toString();
        } catch (SecurityException e) {
            // Missing ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION permission
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    @Keep
    public static boolean IsInternetAvailable(final Activity activity) {
        try {
            Context context = activity;
            ConnectivityManager connectivityManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                return false;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) {
                    return false;
                }
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }

            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * 0 - Undefined
	 * 1 - Light
	 * 2 - Dark
	*/
	@Keep
	public static byte GetCurrentSystemTheme(final Activity activity) {

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			return 0;
		}
        Configuration config = activity.getResources().getConfiguration();
        
		switch (config.uiMode & Configuration.UI_MODE_NIGHT_MASK) {
			case Configuration.UI_MODE_NIGHT_NO:
				return 1;
			case Configuration.UI_MODE_NIGHT_YES:
				return 2;
		}
		return 0;
	}
	
	/** Path to "storage/emulated/0/Android/data/data/%APP_PACKAGE_NAME%/" */
    @Keep
    public static String GetExternalPath(final Activity activity) {
    	Context context = activity;
    	File file = context.getExternalFilesDir(null);
    	String PathStr = file.getPath();
    	PathStr += "/";
    
    	return PathStr;
    }
    
    @Keep
    public static String GetUniqueID(Activity activity) {
    	return Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

	@Keep
	public static String GetOSVersion() {
		return System.getProperty("os.version");
	}

	@Keep
	public static int GetSDKVersion() {
		return Build.VERSION.SDK_INT;
	}

	@Keep
	public static String GetBrand() {
		return Build.BRAND;
	}

	@Keep
	public static String GetModel() {
		return Build.MODEL;
	}

	@Keep
	public static String GetProduct() {
	    return Build.PRODUCT;
	}

	@Keep
	public static String GetLanguage()	{
		return ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0).getDefault().getDisplayLanguage();
	}

	@Keep
	public static String GetLanguageCode()	{
		return ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0).getDefault().toLanguageTag();
	}

	/**
	 * Gather the basic device info in a single JNI round-trip. Order:
	 * [0]=UniqueID, [1]=OSVersion, [2]=SDKVersion, [3]=Brand, [4]=Model,
	 * [5]=Product, [6]=Language, [7]=LanguageCode.
	 */
	@Keep
	public static String[] GetBaseDeviceInfo(final Activity activity) {
		String[] info = new String[8];
		info[0] = GetUniqueID(activity);
		info[1] = GetOSVersion();
		info[2] = String.valueOf(GetSDKVersion());
		info[3] = GetBrand();
		info[4] = GetModel();
		info[5] = GetProduct();
		info[6] = GetLanguage();
		info[7] = GetLanguageCode();
		return info;
	}
	
	@Keep
	public static void CopyToClipboard(final Activity activity, final String text){
		// ClipboardManager must be used on a Looper (UI) thread, so post it there.
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
					if (clipboard == null) {
						return;
					}
					ClipData clip = ClipData.newPlainText("label", text);
					clipboard.setPrimaryClip(clip);
				} catch (Exception e) {
					// Ignore clipboard failures
				}
			}
		});
	}

	/**
	 * Resolve (and cache) the id of a camera that has a flash unit. Prefers a back-facing
	 * camera and falls back to the first camera with a flash. Returns null if none / unsupported.
	 */
	private static String GetFlashCameraId(final Activity activity) {
		if (flashCameraResolved) {
			return flashCameraId;
		}

		flashCameraResolved = true;
		flashCameraId = null;

		try {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
				return null;
			}

			CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
			if (cameraManager == null) {
				return null;
			}

			for (String cameraId : cameraManager.getCameraIdList()) {
				CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
				Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
				if (hasFlash == null || !hasFlash) {
					continue;
				}

				Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
				if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
					flashCameraId = cameraId;
					return flashCameraId;
				}

				if (flashCameraId == null) {
					// Remember the first camera that has a flash as a fallback
					flashCameraId = cameraId;
				}
			}
		} catch (Exception e) {
			flashCameraId = null;
		}

		return flashCameraId;
	}

	/**
	 * Returns true if the device has a camera flash that can be used as a torch (API 23+).
	 */
	@Keep
	public static boolean IsTorchAvailable(final Activity activity) {
		return GetFlashCameraId(activity) != null;
	}

	/**
	 * Toggle the device flashlight (torch). Returns true if the torch state was applied.
	 * Requires API 23 (Marshmallow) or newer and a camera with a flash unit.
	 */
	@Keep
	public static boolean SetTorchEnabled(final Activity activity, boolean enabled) {
		try {
			String cameraId = GetFlashCameraId(activity);
			if (cameraId == null) {
				return false;
			}

			CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
			if (cameraManager == null) {
				return false;
			}

			cameraManager.setTorchMode(cameraId, enabled);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Open the system gallery / photos app so the user can browse images.
	 */
	@Keep
	public static boolean OpenGallery(final Activity activity) {
		Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			activity.startActivity(intent);
			return true;
		} catch (android.content.ActivityNotFoundException e) {
			// No default gallery app: offer a chooser as a fallback
			try {
				Intent chooser = Intent.createChooser(intent, "Open gallery");
				chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				activity.startActivity(chooser);
				return true;
			} catch (Exception ex) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Start a phone call to the given number. Requires the CALL_PHONE permission to be granted
	 * at runtime; otherwise it falls back to opening the dialer pre-filled with the number.
	 */
	@Keep
	public static boolean MakePhoneCall(final Activity activity, String phoneNumber) {
		// Uri.fromParts avoids mis-parsing numbers that contain characters like '#' or '+'.
		Uri phoneUri = Uri.fromParts("tel", phoneNumber, null);
		try {
			Intent intent = new Intent(Intent.ACTION_CALL, phoneUri);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			return true;
		} catch (SecurityException e) {
			// CALL_PHONE permission not granted, fall back to the dialer (never needs a permission)
			try {
				Intent dialIntent = new Intent(Intent.ACTION_DIAL, phoneUri);
				dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				activity.startActivity(dialIntent);
				return true;
			} catch (Exception ex) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Open the system email composer pre-filled with the given recipient, subject and body.
	 */
	@Keep
	public static boolean SendEmail(final Activity activity, String recipient, String subject, String body) {
		try {
			Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
			if (recipient != null && !recipient.isEmpty()) {
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});
			}
			intent.putExtra(Intent.EXTRA_SUBJECT, subject != null ? subject : "");
			intent.putExtra(Intent.EXTRA_TEXT, body != null ? body : "");
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// ---------------------------------------------------------------------------
	// Batch 1: system utilities
	// ---------------------------------------------------------------------------

	/**
	 * Vibrate the device for the given number of milliseconds. Requires the VIBRATE permission.
	 */
	@Keep
	public static void Vibrate(final Activity activity, int milliseconds) {
		try {
			if (milliseconds <= 0) {
				return;
			}

			Vibrator vibrator;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				VibratorManager vibratorManager = (VibratorManager) activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
				vibrator = (vibratorManager != null) ? vibratorManager.getDefaultVibrator() : null;
			} else {
				vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
			}

			if (vibrator == null || !vibrator.hasVibrator()) {
				return;
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
			} else {
				vibrator.vibrate(milliseconds);
			}
		} catch (Exception e) {
			// Ignore
		}
	}

	/**
	 * Show a system Toast message. Runs on the UI thread.
	 */
	@Keep
	public static void ShowToast(final Activity activity, final String message, final boolean longDuration) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					Toast.makeText(activity, message, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					// Ignore
				}
			}
		});
	}

	/**
	 * Open the system share sheet with the given plain text.
	 */
	@Keep
	public static boolean ShareText(final Activity activity, String text, String title) {
		try {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, text != null ? text : "");

			Intent chooser = Intent.createChooser(intent, (title != null && !title.isEmpty()) ? title : "Share");
			chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(chooser);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Battery level as a percentage in [0, 100], or -1 if it can't be read.
	 */
	@Keep
	public static int GetBatteryLevel(final Activity activity) {
		try {
			Intent batteryStatus = activity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			if (batteryStatus == null) {
				return -1;
			}
			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			if (level < 0 || scale <= 0) {
				return -1;
			}
			return Math.round(level * 100f / scale);
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * True if the device is currently charging or already full.
	 */
	@Keep
	public static boolean IsCharging(final Activity activity) {
		try {
			Intent batteryStatus = activity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			if (batteryStatus == null) {
				return false;
			}
			int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Keep the screen on (or release the request). Runs on the UI thread.
	 */
	@Keep
	public static void SetKeepScreenOn(final Activity activity, final boolean keepOn) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					if (keepOn) {
						activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					} else {
						activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					}
				} catch (Exception e) {
					// Ignore
				}
			}
		});
	}

	/**
	 * Open the given URL in the default browser / handler.
	 */
	@Keep
	public static boolean OpenURL(final Activity activity, String url) {
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Open this application's system settings (details) page.
	 */
	@Keep
	public static boolean OpenAppSettings(final Activity activity) {
		try {
			Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
				Uri.fromParts("package", activity.getPackageName(), null));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Application version name (e.g. "1.2.3"), or empty string on failure.
	 */
	@Keep
	public static String GetAppVersion(final Activity activity) {
		try {
			PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
			return packageInfo.versionName != null ? packageInfo.versionName : "";
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Application version code, or -1 on failure.
	 */
	@Keep
	public static int GetAppVersionCode(final Activity activity) {
		try {
			PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				return (int) packageInfo.getLongVersionCode();
			}
			return packageInfo.versionCode;
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Application package name (e.g. "com.company.game").
	 */
	@Keep
	public static String GetPackageName(final Activity activity) {
		try {
			return activity.getPackageName();
		} catch (Exception e) {
			return "";
		}
	}

	// ---------------------------------------------------------------------------
	// Batch 2: system utilities
	// ---------------------------------------------------------------------------

	/** Set the app window brightness. percent in [0,100], or negative to follow the system. UI thread. */
	@Keep
	public static void SetScreenBrightness(final Activity activity, final int percent) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					android.view.WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
					lp.screenBrightness = (percent < 0) ? -1f : Math.max(0, Math.min(100, percent)) / 100f;
					activity.getWindow().setAttributes(lp);
				} catch (Exception e) {
					// Ignore
				}
			}
		});
	}

	/** Current app window brightness as a percentage in [0,100], or -1 if it follows the system. */
	@Keep
	public static int GetScreenBrightness(final Activity activity) {
		try {
			float b = activity.getWindow().getAttributes().screenBrightness;
			if (b < 0) {
				return -1;
			}
			return Math.round(b * 100);
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Set the requested screen orientation. mode:
	 * 0=Unspecified, 1=Portrait, 2=Landscape, 3=ReversePortrait, 4=ReverseLandscape, 5=Sensor. UI thread.
	 */
	@Keep
	public static void SetScreenOrientation(final Activity activity, final int mode) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					int orientation;
					switch (mode) {
						case 1: orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; break;
						case 2: orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; break;
						case 3: orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT; break;
						case 4: orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE; break;
						case 5: orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR; break;
						default: orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED; break;
					}
					activity.setRequestedOrientation(orientation);
				} catch (Exception e) {
					// Ignore
				}
			}
		});
	}

	/** Music-stream volume as a percentage in [0,100], or -1 on failure. */
	@Keep
	public static int GetMusicVolume(final Activity activity) {
		try {
			AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
			if (audioManager == null) {
				return -1;
			}
			int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			if (max <= 0) {
				return -1;
			}
			return Math.round(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / max);
		} catch (Exception e) {
			return -1;
		}
	}

	/** Set the music-stream volume. percent in [0,100]. */
	@Keep
	public static void SetMusicVolume(final Activity activity, int percent) {
		try {
			AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
			if (audioManager == null) {
				return;
			}
			int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			int value = Math.round(Math.max(0, Math.min(100, percent)) * max / 100f);
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
		} catch (Exception e) {
			// May throw if Do Not Disturb policy blocks it; ignore
		}
	}

	/** Active network type: 0=None, 1=WiFi, 2=Cellular, 3=Other. */
	@Keep
	public static int GetNetworkType(final Activity activity) {
		try {
			ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm == null) {
				return 0;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				Network network = cm.getActiveNetwork();
				if (network == null) {
					return 0;
				}
				NetworkCapabilities caps = cm.getNetworkCapabilities(network);
				if (caps == null) {
					return 0;
				}
				if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
					return 1;
				}
				if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
					return 2;
				}
				return 3;
			}

			NetworkInfo networkInfo = cm.getActiveNetworkInfo();
			if (networkInfo == null || !networkInfo.isConnected()) {
				return 0;
			}
			int type = networkInfo.getType();
			if (type == ConnectivityManager.TYPE_WIFI) {
				return 1;
			}
			if (type == ConnectivityManager.TYPE_MOBILE) {
				return 2;
			}
			return 3;
		} catch (Exception e) {
			return 0;
		}
	}

	/** Total physical RAM in MB, or -1 on failure. */
	@Keep
	public static int GetTotalMemoryMB(final Activity activity) {
		try {
			ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
			if (am == null) {
				return -1;
			}
			ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
			am.getMemoryInfo(mi);
			return (int) (mi.totalMem / (1024 * 1024));
		} catch (Exception e) {
			return -1;
		}
	}

	/** Available RAM in MB, or -1 on failure. */
	@Keep
	public static int GetAvailableMemoryMB(final Activity activity) {
		try {
			ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
			if (am == null) {
				return -1;
			}
			ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
			am.getMemoryInfo(mi);
			return (int) (mi.availMem / (1024 * 1024));
		} catch (Exception e) {
			return -1;
		}
	}

	/** Total internal storage in MB, or -1 on failure. */
	@Keep
	public static int GetTotalStorageMB(final Activity activity) {
		try {
			StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
			long bytes = stat.getBlockCountLong() * stat.getBlockSizeLong();
			return (int) (bytes / (1024 * 1024));
		} catch (Exception e) {
			return -1;
		}
	}

	/** Available internal storage in MB, or -1 on failure. */
	@Keep
	public static int GetAvailableStorageMB(final Activity activity) {
		try {
			StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
			long bytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
			return (int) (bytes / (1024 * 1024));
		} catch (Exception e) {
			return -1;
		}
	}

	/** Status bar height in pixels (0 if unknown). */
	@Keep
	public static int GetStatusBarHeight(final Activity activity) {
		try {
			int id = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
			return id > 0 ? activity.getResources().getDimensionPixelSize(id) : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	/** Navigation bar height in pixels (0 if unknown). */
	@Keep
	public static int GetNavigationBarHeight(final Activity activity) {
		try {
			int id = activity.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
			return id > 0 ? activity.getResources().getDimensionPixelSize(id) : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	/** Enable/disable sticky immersive fullscreen. UI thread. */
	@Keep
	public static void SetImmersiveMode(final Activity activity, final boolean enabled) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					android.view.View decor = activity.getWindow().getDecorView();
					if (enabled) {
						decor.setSystemUiVisibility(
							android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
								| android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
								| android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
								| android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
								| android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
								| android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
					} else {
						decor.setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_VISIBLE);
					}
				} catch (Exception e) {
					// Ignore
				}
			}
		});
	}

	/** Open a geo: URI (maps). The C++ side builds the URI from coordinates/label. */
	@Keep
	public static boolean OpenGeoUri(final Activity activity, String uri) {
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** Read the current clipboard text (empty string if none). */
	@Keep
	public static String GetClipboardText(final Activity activity) {
		try {
			ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
			if (clipboard == null || !clipboard.hasPrimaryClip()) {
				return "";
			}
			ClipData clip = clipboard.getPrimaryClip();
			if (clip == null || clip.getItemCount() == 0) {
				return "";
			}
			CharSequence text = clip.getItemAt(0).coerceToText(activity);
			return text != null ? text.toString() : "";
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * True if the given package is installed. Note: on Android 11+ this is subject to
	 * package visibility rules unless the target is declared in <queries> or is otherwise visible.
	 */
	@Keep
	public static boolean IsAppInstalled(final Activity activity, String packageName) {
		try {
			activity.getPackageManager().getPackageInfo(packageName, 0);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** Launch another installed app by package name. Subject to Android 11+ package visibility. */
	@Keep
	public static boolean OpenApp(final Activity activity, String packageName) {
		try {
			Intent intent = activity.getPackageManager().getLaunchIntentForPackage(packageName);
			if (intent == null) {
				return false;
			}
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** True if the given runtime permission (e.g. "android.permission.CAMERA") is currently granted. */
	@Keep
	public static boolean IsPermissionGranted(final Activity activity, String permission) {
		try {
			return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Request runtime permissions. Already-granted permissions resolve immediately via
	 * nativeOnPermissionResult; the rest are requested and their result is delivered from
	 * GameActivity.onRequestPermissionsResult (see the UPL additions).
	 */
	@Keep
	public static void RequestPermissions(final Activity activity, final String[] permissions) {
		try {
			java.util.ArrayList<String> toRequest = new java.util.ArrayList<String>();
			for (String p : permissions) {
				if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
					toRequest.add(p);
				}
			}

			if (toRequest.isEmpty()) {
				boolean[] granted = new boolean[permissions.length];
				for (int i = 0; i < permissions.length; i++) {
					granted[i] = true;
				}
				nativeOnPermissionResult(permissions, granted);
				return;
			}

			ActivityCompat.requestPermissions(activity, toRequest.toArray(new String[0]), 11011);
		} catch (Exception e) {
			nativeOnPermissionResult(permissions, new boolean[permissions.length]);
		}
	}

	/** Launch an image picker; the result arrives via nativeOnActivityResult(requestCode, ...). */
	@Keep
	public static void PickImageFromGallery(final Activity activity, int requestCode) {
		try {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			activity.startActivityForResult(intent, requestCode);
		} catch (Exception e) {
			nativeOnActivityResult(requestCode, 0, "");
		}
	}

	/** Launch a file picker with the given MIME type (defaults to any). */
	@Keep
	public static void PickFile(final Activity activity, int requestCode, String mimeType) {
		try {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType((mimeType != null && !mimeType.isEmpty()) ? mimeType : "*/*");
			activity.startActivityForResult(intent, requestCode);
		} catch (Exception e) {
			nativeOnActivityResult(requestCode, 0, "");
		}
	}

	/** Start streaming a sensor (sensorType is an android.hardware.Sensor.TYPE_* value). */
	@Keep
	public static boolean StartSensor(final Activity activity, final int sensorType) {
		try {
			final SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
			if (sensorManager == null) {
				return false;
			}
			Sensor sensor = sensorManager.getDefaultSensor(sensorType);
			if (sensor == null) {
				return false;
			}
			if (sensorListeners.containsKey(sensorType)) {
				return true;
			}

			SensorEventListener listener = new SensorEventListener() {
				@Override
				public void onSensorChanged(SensorEvent event) {
					float x = event.values.length > 0 ? event.values[0] : 0f;
					float y = event.values.length > 1 ? event.values[1] : 0f;
					float z = event.values.length > 2 ? event.values[2] : 0f;
					nativeOnSensorChanged(sensorType, x, y, z);
				}

				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {}
			};

			sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME);
			sensorListeners.put(sensorType, listener);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** Stop streaming a previously started sensor. */
	@Keep
	public static void StopSensor(final Activity activity, final int sensorType) {
		try {
			SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
			SensorEventListener listener = sensorListeners.remove(sensorType);
			if (sensorManager != null && listener != null) {
				sensorManager.unregisterListener(listener);
			}
		} catch (Exception e) {
			// Ignore
		}
	}

	/**
	 * Post a simple notification. On Android 13+ this requires the POST_NOTIFICATIONS
	 * runtime permission to actually be shown.
	 */
	@Keep
	public static void ShowNotification(final Activity activity, String title, String text, int notificationId) {
		try {
			final String channelId = "androidnative_default";
			NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
			if (manager == null) {
				return;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(channelId, "General", NotificationManager.IMPORTANCE_DEFAULT);
				manager.createNotificationChannel(channel);
			}

			int icon = activity.getApplicationInfo().icon;
			NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, channelId)
				.setSmallIcon(icon != 0 ? icon : android.R.drawable.ic_dialog_info)
				.setContentTitle(title != null ? title : "")
				.setContentText(text != null ? text : "")
				.setAutoCancel(true);

			NotificationManagerCompat.from(activity).notify(notificationId, builder.build());
		} catch (Exception e) {
			// Ignore
		}
	}

	/** True if the app is currently allowed to post notifications. */
	@Keep
	public static boolean AreNotificationsEnabled(final Activity activity) {
		try {
			return NotificationManagerCompat.from(activity).areNotificationsEnabled();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Request a single fresh location fix. Requires ACCESS_FINE_LOCATION or
	 * ACCESS_COARSE_LOCATION to be granted. The result is delivered via nativeOnLocationResult.
	 */
	@Keep
	public static void RequestSingleLocation(final Activity activity, final int requestId) {
		try {
			final LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
			if (locationManager == null) {
				nativeOnLocationResult(requestId, false, 0.0, 0.0);
				return;
			}

			final String provider;
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				provider = LocationManager.GPS_PROVIDER;
			} else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				provider = LocationManager.NETWORK_PROVIDER;
			} else {
				nativeOnLocationResult(requestId, false, 0.0, 0.0);
				return;
			}

			final LocationListener[] holder = new LocationListener[1];
			LocationListener listener = new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					try { locationManager.removeUpdates(holder[0]); } catch (Exception e) {}
					nativeOnLocationResult(requestId, true, location.getLatitude(), location.getLongitude());
				}

				@Override public void onStatusChanged(String p, int s, android.os.Bundle b) {}
				@Override public void onProviderEnabled(String p) {}
				@Override public void onProviderDisabled(String p) {}
			};
			holder[0] = listener;

			// requestSingleUpdate with a null Looper uses the calling thread's looper, so run it on the UI thread.
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						locationManager.requestSingleUpdate(provider, holder[0], null);
					} catch (Exception e) {
						nativeOnLocationResult(requestId, false, 0.0, 0.0);
					}
				}
			});
		} catch (Exception e) {
			nativeOnLocationResult(requestId, false, 0.0, 0.0);
		}
	}
}