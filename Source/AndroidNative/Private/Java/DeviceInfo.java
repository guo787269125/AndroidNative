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

@Keep
	public static void startNsdService(final Activity activity, int Port) {
				
		context = activity;
				
		nsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);

		nsdServiceInfo = new NsdServiceInfo();
		nsdServiceInfo.setServiceName("NdiNsdService");
		nsdServiceInfo.setServiceType("_ndi._tcp.");
		nsdServiceInfo.setPort(Port);

		nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, new NsdManager.RegistrationListener() {

			@Override
			public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {}

			@Override
			public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {}

			@Override
			public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {}

			@Override
			public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {}
		});
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
	
	@Keep
	public static void CopyToClipboard(final Activity activity, String text){
	    Context context = activity;
	    
	    // Get the ClipboardManager service
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                
        // Create a new ClipData with the specified text
        ClipData clip = ClipData.newPlainText("label", text);
                
        // Set the clipboard's primary clip
        clipboard.setPrimaryClip(clip);
	}

	/**
	 * Toggle the device flashlight (torch). Returns true if the torch state was applied.
	 * Requires API 23 (Marshmallow) or newer.
	 */
	@Keep
	public static boolean SetTorchEnabled(final Activity activity, boolean enabled) {
		try {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
				return false;
			}

			CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
			if (cameraManager == null) {
				return false;
			}

			String targetCameraId = null;
			for (String cameraId : cameraManager.getCameraIdList()) {
				CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
				Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
				if (hasFlash == null || !hasFlash) {
					continue;
				}

				Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
				if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
					targetCameraId = cameraId;
					break;
				}

				if (targetCameraId == null) {
					// Remember the first camera that has a flash as a fallback
					targetCameraId = cameraId;
				}
			}

			if (targetCameraId == null) {
				return false;
			}

			cameraManager.setTorchMode(targetCameraId, enabled);
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
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			return true;
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
		try {
			Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			return true;
		} catch (SecurityException e) {
			// CALL_PHONE permission not granted, fall back to the dialer (never needs a permission)
			try {
				Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
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