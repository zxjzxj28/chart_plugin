package com.yourcompany.a11y;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ContentProvider that automatically initializes chart accessibility features.
 *
 * <p>This provider is automatically registered and will initialize all XML-configured
 * charts without requiring any code from the application developer.</p>
 *
 * <h3>How it works:</h3>
 * <ul>
 *   <li>The ContentProvider is automatically instantiated before Application.onCreate()</li>
 *   <li>It registers an ActivityLifecycleCallbacks to monitor all activities</li>
 *   <li>When an activity is resumed, it automatically initializes any charts with a11y attributes</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <p>Just add the a11y attributes to your chart views in XML - no code needed!</p>
 * <pre>
 * &lt;com.github.mikephil.charting.charts.BarChart
 *     android:id="@+id/barChart"
 *     android:layout_width="match_parent"
 *     android:layout_height="300dp"
 *     app:a11yChartId="sales_quarterly"
 *     app:a11yEnableNavigation="true" /&gt;
 * </pre>
 *
 * <h3>Disabling Auto-Initialization:</h3>
 * <p>If you need to manually control initialization, add this meta-data to your manifest:</p>
 * <pre>
 * &lt;meta-data
 *     android:name="com.yourcompany.a11y.AUTO_INIT_DISABLED"
 *     android:value="true" /&gt;
 * </pre>
 */
public class ChartA11yInitializer extends ContentProvider {

    private static final String TAG = "ChartA11yInitializer";
    private static final String META_DATA_AUTO_INIT_DISABLED = "com.yourcompany.a11y.AUTO_INIT_DISABLED";

    private static boolean initialized = false;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Context is null, cannot initialize");
            return false;
        }

        // Check if auto-initialization is disabled via manifest meta-data
        if (isAutoInitDisabled(context)) {
            Log.d(TAG, "Auto-initialization is disabled via manifest meta-data");
            return true;
        }

        Application application = (Application) context.getApplicationContext();
        registerActivityCallbacks(application);

        initialized = true;
        Log.d(TAG, "Chart accessibility auto-initialization enabled");
        return true;
    }

    /**
     * Check if auto-initialization is disabled via manifest meta-data.
     */
    private boolean isAutoInitDisabled(@NonNull Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

            if (appInfo.metaData != null) {
                return appInfo.metaData.getBoolean(META_DATA_AUTO_INIT_DISABLED, false);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed to read manifest meta-data", e);
        }
        return false;
    }

    /**
     * Register activity lifecycle callbacks to automatically initialize charts.
     */
    private void registerActivityCallbacks(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(new ChartA11yActivityCallbacks());
    }

    /**
     * Check if the auto-initializer has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    // ContentProvider required methods - not used for data storage

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
