// Georgy Treshchev 2024.

package com.Plugins.AndroidNative;

import androidx.annotation.Keep;
import android.app.Activity;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;

/**
 * Google Play in-app review flow. Requires the com.google.android.play:review dependency
 * (added by the UPL). The flow is a no-op quota-limited overlay on real Play installs.
 */
@Keep
public class ReviewHelper {

	@Keep public static native void nativeOnReviewComplete(int requestId, boolean success);

	@Keep
	public static void RequestReview(final Activity activity, final int requestId) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					final ReviewManager manager = ReviewManagerFactory.create(activity);
					Task<ReviewInfo> request = manager.requestReviewFlow();
					request.addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
						@Override
						public void onComplete(Task<ReviewInfo> task) {
							if (task.isSuccessful()) {
								ReviewInfo reviewInfo = task.getResult();
								Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
								flow.addOnCompleteListener(new OnCompleteListener<Void>() {
									@Override
									public void onComplete(Task<Void> t) {
										nativeOnReviewComplete(requestId, true);
									}
								});
							} else {
								nativeOnReviewComplete(requestId, false);
							}
						}
					});
				} catch (Exception e) {
					nativeOnReviewComplete(requestId, false);
				}
			}
		});
	}
}
