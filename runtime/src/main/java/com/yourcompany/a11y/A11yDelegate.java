package com.yourcompany.a11y;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom AccessibilityDelegate for chart views.
 *
 * Features:
 * - Sets roleDescription for the chart type
 * - Provides data point navigation via custom actions
 * - Reports state description (e.g., "item 2 of 4")
 */
public class A11yDelegate extends AccessibilityDelegateCompat {

    private static final int ACTION_NEXT_DATA_POINT = 0x10001;
    private static final int ACTION_PREVIOUS_DATA_POINT = 0x10002;

    private final String chartId;
    private final String roleDescription;
    private final List<String> dataPointDescriptions;
    private int currentDataPointIndex = 0;

    /**
     * Create an accessibility delegate for a chart.
     *
     * @param chartId the chart identifier
     * @param roleDescription the role description (e.g., "bar chart")
     */
    public A11yDelegate(@NonNull String chartId, @Nullable String roleDescription) {
        this.chartId = chartId;
        this.roleDescription = roleDescription;
        this.dataPointDescriptions = new ArrayList<>();
    }

    /**
     * Load data point descriptions from resources.
     *
     * @param context the context
     * @param resourcePrefix the resource name prefix (e.g., "a11y_chart_sales_quarterly_point_")
     */
    public void loadDataPoints(@NonNull Context context, @NonNull String resourcePrefix) {
        dataPointDescriptions.clear();
        Resources resources = context.getResources();
        String packageName = context.getPackageName();

        // Try to load data points (point_0, point_1, etc.)
        for (int i = 0; i < 100; i++) { // Reasonable limit
            String resourceName = resourcePrefix + i;
            int resId = resources.getIdentifier(resourceName, "string", packageName);
            if (resId == 0) {
                break; // No more data points
            }
            dataPointDescriptions.add(resources.getString(resId));
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                   @NonNull AccessibilityNodeInfoCompat info) {
        super.onInitializeAccessibilityNodeInfo(host, info);

        // Set role description
        if (roleDescription != null && !roleDescription.isEmpty()) {
            info.setRoleDescription(roleDescription);
        }

        // Set class name for better TalkBack support
        info.setClassName("android.widget.ImageView");

        // Add custom actions for data point navigation if available
        if (!dataPointDescriptions.isEmpty()) {
            // Add "Next data point" action
            if (currentDataPointIndex < dataPointDescriptions.size() - 1) {
                AccessibilityNodeInfoCompat.AccessibilityActionCompat nextAction =
                        new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                ACTION_NEXT_DATA_POINT, "Next data point");
                info.addAction(nextAction);
            }

            // Add "Previous data point" action
            if (currentDataPointIndex > 0) {
                AccessibilityNodeInfoCompat.AccessibilityActionCompat prevAction =
                        new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                ACTION_PREVIOUS_DATA_POINT, "Previous data point");
                info.addAction(prevAction);
            }
        }
    }

    @Override
    public boolean performAccessibilityAction(@NonNull View host, int action,
                                               @Nullable Bundle args) {
        switch (action) {
            case ACTION_NEXT_DATA_POINT:
                if (currentDataPointIndex < dataPointDescriptions.size() - 1) {
                    currentDataPointIndex++;
                    announceDataPoint(host);
                    return true;
                }
                break;

            case ACTION_PREVIOUS_DATA_POINT:
                if (currentDataPointIndex > 0) {
                    currentDataPointIndex--;
                    announceDataPoint(host);
                    return true;
                }
                break;
        }

        return super.performAccessibilityAction(host, action, args);
    }

    @Override
    public void onPopulateAccessibilityEvent(@NonNull View host,
                                              @NonNull AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(host, event);

        // Add state description when data points are available
        if (!dataPointDescriptions.isEmpty() && event.getEventType() ==
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
            String stateDesc = getStateDescription();
            event.getText().add(stateDesc);
        }
    }

    /**
     * Get the state description showing current position in data points.
     *
     * @return state description string
     */
    public String getStateDescription() {
        if (dataPointDescriptions.isEmpty()) {
            return "";
        }
        return String.format("Item %d of %d",
                currentDataPointIndex + 1, dataPointDescriptions.size());
    }

    /**
     * Get the current data point description.
     *
     * @return current data point description, or null if none
     */
    @Nullable
    public String getCurrentDataPointDescription() {
        if (dataPointDescriptions.isEmpty() ||
                currentDataPointIndex >= dataPointDescriptions.size()) {
            return null;
        }
        return dataPointDescriptions.get(currentDataPointIndex);
    }

    /**
     * Navigate to a specific data point.
     *
     * @param index the data point index
     */
    public void navigateToDataPoint(int index) {
        if (index >= 0 && index < dataPointDescriptions.size()) {
            currentDataPointIndex = index;
        }
    }

    /**
     * Reset navigation to the first data point.
     */
    public void resetNavigation() {
        currentDataPointIndex = 0;
    }

    /**
     * Get the number of data points.
     *
     * @return data point count
     */
    public int getDataPointCount() {
        return dataPointDescriptions.size();
    }

    private void announceDataPoint(View host) {
        String description = getCurrentDataPointDescription();
        if (description != null) {
            host.announceForAccessibility(description + ". " + getStateDescription());
        }
    }
}
