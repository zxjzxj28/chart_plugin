package com.yourcompany.a11y;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/**
 * Chart Accessibility SDK entry point.
 *
 * Provides easy-to-use APIs for applying accessibility features to chart views.
 *
 * <h3>Simple Usage:</h3>
 * <pre>
 * ChartA11y.apply(view, "chart_id");
 * </pre>
 *
 * <h3>With Description Type:</h3>
 * <pre>
 * ChartA11y.apply(view, "chart_id", DescType.DETAILED);
 * </pre>
 *
 * <h3>Builder Pattern:</h3>
 * <pre>
 * ChartA11y.with(view)
 *     .chartId("chart_id")
 *     .descType(DescType.DETAILED)
 *     .focusable(true)
 *     .roleDescription("柱状图")
 *     .apply();
 * </pre>
 */
public class ChartA11y {

    private static final String TAG = "ChartA11y";
    private static final String RESOURCE_PREFIX = "a11y_chart_";
    private static final String A11Y_TAG_PREFIX = "a11y:";

    private ChartA11y() {
        // Utility class
    }

    /**
     * Initialize all chart views configured with a11y attributes in XML layouts.
     * This method should be called in Activity.onCreate() after setContentView().
     *
     * <p>Example usage:</p>
     * <pre>
     * {@literal @}Override
     * protected void onCreate(Bundle savedInstanceState) {
     *     super.onCreate(savedInstanceState);
     *     setContentView(R.layout.activity_main);
     *
     *     // Initialize all XML-configured charts with data point navigation
     *     ChartA11y.initializeFromXml(this);
     * }
     * </pre>
     *
     * @param activity the activity containing chart views
     * @return the number of views initialized
     */
    public static int initializeFromXml(@NonNull Activity activity) {
        View rootView = activity.getWindow().getDecorView().getRootView();
        return initializeFromXml(rootView);
    }

    /**
     * Initialize all chart views configured with a11y attributes within a view hierarchy.
     *
     * @param rootView the root view to search within
     * @return the number of views initialized
     */
    public static int initializeFromXml(@NonNull View rootView) {
        int[] count = {0};
        traverseViewHierarchy(rootView, view -> {
            if (initializeViewFromTag(view)) {
                count[0]++;
            }
        });
        return count[0];
    }

    /**
     * Check if a view has a11y configuration and initialize it if so.
     *
     * @param view the view to check
     * @return true if the view was initialized
     */
    private static boolean initializeViewFromTag(@NonNull View view) {
        Object tag = view.getTag();
        if (!(tag instanceof String)) {
            return false;
        }

        String tagStr = (String) tag;
        if (!tagStr.startsWith(A11Y_TAG_PREFIX)) {
            return false;
        }

        // Parse tag: "a11y:{chartId}:{descType}:{enableNavigation}"
        String[] parts = tagStr.substring(A11Y_TAG_PREFIX.length()).split(":");
        if (parts.length < 3) {
            Log.w(TAG, "Invalid a11y tag format: " + tagStr);
            return false;
        }

        String chartId = parts[0];
        String descTypeStr = parts[1];
        boolean enableNavigation = "true".equalsIgnoreCase(parts[2]);

        if (!enableNavigation) {
            return false;
        }

        // Determine description type
        DescType descType = "detailed".equalsIgnoreCase(descTypeStr)
                ? DescType.DETAILED : DescType.BRIEF;

        // Apply data point navigation
        with(view)
                .chartId(chartId)
                .descType(descType)
                .enableDataPointNavigation(true)
                .apply();

        Log.d(TAG, "Initialized data point navigation for chart: " + chartId);
        return true;
    }

    /**
     * Traverse the view hierarchy and apply an action to each view.
     */
    private static void traverseViewHierarchy(@NonNull View view, @NonNull ViewAction action) {
        action.apply(view);

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                traverseViewHierarchy(group.getChildAt(i), action);
            }
        }
    }

    /** Functional interface for view traversal */
    private interface ViewAction {
        void apply(View view);
    }

    /**
     * Apply accessibility features to a view with brief description.
     *
     * @param view the chart view
     * @param chartId the chart identifier
     */
    public static void apply(@NonNull View view, @NonNull String chartId) {
        apply(view, chartId, DescType.BRIEF);
    }

    /**
     * Apply accessibility features to a view with specified description type.
     *
     * @param view the chart view
     * @param chartId the chart identifier
     * @param descType the description type (brief or detailed)
     */
    public static void apply(@NonNull View view, @NonNull String chartId,
                             @NonNull DescType descType) {
        with(view)
                .chartId(chartId)
                .descType(descType)
                .apply();
    }

    /**
     * Create a builder for configuring accessibility features.
     *
     * @param view the chart view
     * @return the builder instance
     */
    public static Builder with(@NonNull View view) {
        return new Builder(view);
    }

    /**
     * Convert chart ID to resource name.
     * - Convert to lowercase
     * - Replace non-alphanumeric characters with underscores
     * - Merge consecutive underscores
     * - Remove leading and trailing underscores
     */
    static String toResourceName(String chartId) {
        if (chartId == null || chartId.isEmpty()) {
            return "unknown";
        }
        String result = chartId.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return result.isEmpty() ? "unknown" : result;
    }

    /**
     * Builder for configuring chart accessibility features.
     */
    public static class Builder {
        private final View view;
        private String chartId;
        private DescType descType = DescType.BRIEF;
        private boolean focusable = true;
        private String roleDescription;
        private boolean enableDataPointNavigation = true;

        Builder(View view) {
            this.view = view;
        }

        /**
         * Set the chart identifier.
         *
         * @param chartId the chart ID matching the configuration
         * @return this builder
         */
        public Builder chartId(@NonNull String chartId) {
            this.chartId = chartId;
            return this;
        }

        /**
         * Set the description type.
         *
         * @param descType BRIEF or DETAILED
         * @return this builder
         */
        public Builder descType(@NonNull DescType descType) {
            this.descType = descType;
            return this;
        }

        /**
         * Set whether the view should be focusable for accessibility.
         *
         * @param focusable true to make focusable
         * @return this builder
         */
        public Builder focusable(boolean focusable) {
            this.focusable = focusable;
            return this;
        }

        /**
         * Set a custom role description (e.g., "柱状图", "bar chart").
         *
         * @param roleDescription the role description
         * @return this builder
         */
        public Builder roleDescription(@Nullable String roleDescription) {
            this.roleDescription = roleDescription;
            return this;
        }

        /**
         * Enable or disable data point navigation.
         *
         * @param enable true to enable data point navigation
         * @return this builder
         */
        public Builder enableDataPointNavigation(boolean enable) {
            this.enableDataPointNavigation = enable;
            return this;
        }

        /**
         * Apply the accessibility configuration to the view.
         */
        // ChartA11y.java 中 apply() 方法的关键修复

        public void apply() {
            if (chartId == null || chartId.isEmpty()) {
                throw new IllegalStateException("chartId must be set");
            }

            Context context = view.getContext();
            Resources resources = context.getResources();
            String packageName = context.getPackageName();
            String resourceName = toResourceName(chartId);

            // 设置 contentDescription
            String descResourceName = RESOURCE_PREFIX + resourceName + "_" + descType.getSuffix();
            int descResId = resources.getIdentifier(descResourceName, "string", packageName);
            if (descResId != 0) {
                view.setContentDescription(resources.getString(descResId));
            }

            // 关键修复：确保 View 的无障碍属性正确配置
            view.setFocusable(focusable);
            view.setClickable(true);  // 添加这行！
            view.setLongClickable(true);  // 添加这行！

            ViewCompat.setImportantForAccessibility(view,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

            // 设置 LiveRegion 以便状态变化时自动播报
            ViewCompat.setAccessibilityLiveRegion(view,
                    ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);

            // 创建并配置 delegate
            A11yDelegate delegate = new A11yDelegate(chartId, roleDescription);

            if (enableDataPointNavigation) {
                String dataPointPrefix = RESOURCE_PREFIX + resourceName + "_point_";
                delegate.loadDataPoints(context, dataPointPrefix);

                // 如果资源加载失败，提供调试警告
                if (delegate.getDataPointCount() == 0) {
                    Log.w("ChartA11y", "No data points loaded for chart: " + chartId +
                            ". Check resource naming: " + dataPointPrefix + "0, " + dataPointPrefix + "1, ...");
                }
            }

            ViewCompat.setAccessibilityDelegate(view, delegate);
        }
    }
}
