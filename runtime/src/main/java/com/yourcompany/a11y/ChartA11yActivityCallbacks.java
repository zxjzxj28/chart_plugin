package com.yourcompany.a11y;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * Activity lifecycle callbacks that automatically initialize chart accessibility.
 *
 * <p>This class monitors activity lifecycle events and initializes chart accessibility
 * features when activities are resumed. It uses a ViewTreeObserver to ensure views
 * are fully laid out before initialization.</p>
 */
class ChartA11yActivityCallbacks implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "ChartA11yCallbacks";

    // Track activities that have been initialized to avoid duplicate processing
    private final Set<Integer> initializedActivities = new HashSet<>();

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // Not used - initialization happens in onActivityResumed for better timing
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        // Not used
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        int activityHash = System.identityHashCode(activity);

        // Skip if already initialized this activity instance
        if (initializedActivities.contains(activityHash)) {
            return;
        }

        // Use ViewTreeObserver to wait for layout to complete
        View decorView = activity.getWindow().getDecorView();
        if (decorView.isLaidOut()) {
            // View is already laid out, initialize immediately
            initializeActivity(activity, activityHash);
        } else {
            // Wait for layout to complete
            decorView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new LayoutListener(activity, activityHash, this));
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // Not used
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        // Not used
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // Not used
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        // Clean up tracking for destroyed activities
        int activityHash = System.identityHashCode(activity);
        initializedActivities.remove(activityHash);
    }

    /**
     * Initialize chart accessibility for an activity.
     */
    void initializeActivity(@NonNull Activity activity, int activityHash) {
        if (initializedActivities.contains(activityHash)) {
            return;
        }

        try {
            int count = ChartA11y.initializeAllFromXml(activity);
            initializedActivities.add(activityHash);

            if (count > 0) {
                Log.d(TAG, "Auto-initialized " + count + " chart(s) in " +
                        activity.getClass().getSimpleName());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to auto-initialize charts in " +
                    activity.getClass().getSimpleName(), e);
        }
    }

    /**
     * ViewTreeObserver listener that initializes charts after layout.
     */
    private static class LayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {

        private final WeakReference<Activity> activityRef;
        private final int activityHash;
        private final ChartA11yActivityCallbacks callbacks;

        LayoutListener(@NonNull Activity activity, int activityHash,
                       @NonNull ChartA11yActivityCallbacks callbacks) {
            this.activityRef = new WeakReference<>(activity);
            this.activityHash = activityHash;
            this.callbacks = callbacks;
        }

        @Override
        public void onGlobalLayout() {
            Activity activity = activityRef.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            // Remove listener to prevent multiple calls
            View decorView = activity.getWindow().getDecorView();
            decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            // Initialize charts
            callbacks.initializeActivity(activity, activityHash);
        }
    }
}
