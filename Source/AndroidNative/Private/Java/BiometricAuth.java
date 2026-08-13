// Georgy Treshchev 2024.

package com.Plugins.AndroidNative;

import androidx.annotation.Keep;
import android.app.Activity;

import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

/**
 * Biometric (fingerprint/face) authentication via androidx.biometric.
 *
 * IMPORTANT: BiometricPrompt requires the host Activity to be a FragmentActivity.
 * Unreal's GameActivity usually is NOT a FragmentActivity, so Authenticate() will
 * report an error unless your GameActivity is adapted. This class is provided as a
 * starting point; verify/adjust on device.
 */
@Keep
public class BiometricAuth {

	@Keep public static native void nativeOnBiometricResult(int requestId, boolean success, String error);

	/** True if the device has enrolled biometrics that can be used to authenticate. */
	@Keep
	public static boolean IsBiometricAvailable(final Activity activity) {
		try {
			BiometricManager biometricManager = BiometricManager.from(activity);
			int result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
			return result == BiometricManager.BIOMETRIC_SUCCESS;
		} catch (Exception e) {
			return false;
		}
	}

	/** Show the biometric prompt. Result is delivered via nativeOnBiometricResult. */
	@Keep
	public static void Authenticate(final Activity activity, final int requestId, final String title, final String subtitle, final String cancelText) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					if (!(activity instanceof FragmentActivity)) {
						nativeOnBiometricResult(requestId, false, "Host Activity is not a FragmentActivity");
						return;
					}

					FragmentActivity fragmentActivity = (FragmentActivity) activity;

					BiometricPrompt prompt = new BiometricPrompt(fragmentActivity,
						ContextCompat.getMainExecutor(activity),
						new BiometricPrompt.AuthenticationCallback() {
							@Override
							public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
								nativeOnBiometricResult(requestId, true, "");
							}

							@Override
							public void onAuthenticationError(int errorCode, CharSequence errString) {
								nativeOnBiometricResult(requestId, false, errString != null ? errString.toString() : "error");
							}

							@Override
							public void onAuthenticationFailed() {
								// Non-fatal single-attempt failure; wait for success or a terminal error.
							}
						});

					BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
						.setTitle(title != null ? title : "Authenticate")
						.setSubtitle(subtitle != null ? subtitle : "")
						.setNegativeButtonText((cancelText != null && !cancelText.isEmpty()) ? cancelText : "Cancel")
						.build();

					prompt.authenticate(promptInfo);
				} catch (Exception e) {
					nativeOnBiometricResult(requestId, false, e.getMessage() != null ? e.getMessage() : "exception");
				}
			}
		});
	}
}
