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
}