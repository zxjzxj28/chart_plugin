package com.yourcompany.a11y;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

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

    private static final String RESOURCE_PREFIX = "a11y_chart_";

    private ChartA11y() {
        // Utility class
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
        private boolean useVirtualNodes = true; // Default to virtual nodes for gesture support

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
         * Use virtual nodes for data point navigation.
         *
         * When enabled (default), each data point becomes a virtual accessibility node,
         * allowing TalkBack single-finger swipe gestures to navigate between data points.
         *
         * When disabled, uses scroll actions which require two-finger swipe gestures.
         *
         * @param useVirtual true to use virtual nodes (single-finger swipe support)
         * @return this builder
         */
        public Builder useVirtualNodes(boolean useVirtual) {
            this.useVirtualNodes = useVirtual;
            return this;
        }

        /**
         * Apply the accessibility configuration to the view.
         */
        @SuppressLint("ClickableViewAccessibility")
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

            // 确保 View 的无障碍属性正确配置
            view.setFocusable(focusable);
            view.setClickable(true);
            view.setLongClickable(true);

            ViewCompat.setImportantForAccessibility(view,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

            // 设置 LiveRegion 以便状态变化时自动播报
            ViewCompat.setAccessibilityLiveRegion(view,
                    ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);

            String dataPointPrefix = RESOURCE_PREFIX + resourceName + "_point_";

            if (enableDataPointNavigation && useVirtualNodes) {
                // 使用 ExploreByTouchHelper 实现虚拟节点导航
                // 这支持 TalkBack 单指滑动手势在数据点之间导航
                final ChartExploreByTouchHelper touchHelper =
                        new ChartExploreByTouchHelper(view, chartId, roleDescription);
                touchHelper.loadDataPoints(context, dataPointPrefix);

                if (touchHelper.getDataPointCount() == 0) {
                    Log.w("ChartA11y", "No data points loaded for chart: " + chartId +
                            ". Check resource naming: " + dataPointPrefix + "0, " + dataPointPrefix + "1, ...");
                } else {
                    Log.d("ChartA11y", "Using virtual nodes for " + touchHelper.getDataPointCount() +
                            " data points. Single-finger swipe enabled.");
                }

                ViewCompat.setAccessibilityDelegate(view, touchHelper);

                // 设置触摸监听器以支持触摸探索
                view.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return touchHelper.dispatchHoverEvent(event);
                    }
                });

                // 在布局完成后刷新边界
                view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        touchHelper.refreshAccessibilityInfo();
                    }
                });

            } else if (enableDataPointNavigation) {
                // 使用传统的 A11yDelegate（需要两指滑动或菜单操作）
                A11yDelegate delegate = new A11yDelegate(chartId, roleDescription);
                delegate.loadDataPoints(context, dataPointPrefix);

                if (delegate.getDataPointCount() == 0) {
                    Log.w("ChartA11y", "No data points loaded for chart: " + chartId +
                            ". Check resource naming: " + dataPointPrefix + "0, " + dataPointPrefix + "1, ...");
                }

                ViewCompat.setAccessibilityDelegate(view, delegate);
            } else {
                // 仅设置基本的角色描述
                A11yDelegate delegate = new A11yDelegate(chartId, roleDescription);
                ViewCompat.setAccessibilityDelegate(view, delegate);
            }
        }
    }
}
