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
 * - Provides data point navigation via custom actions (scroll forward/backward)
 * - Reports state description (e.g., "item 2 of 4")
 *
 * User interaction with TalkBack:
 * - Swipe right with two fingers or use "Scroll forward" action: Next data point
 * - Swipe left with two fingers or use "Scroll backward" action: Previous data point
 */
public class A11yDelegate extends AccessibilityDelegateCompat {

    // Use standard scroll actions for better TalkBack compatibility
    // These are recognized by TalkBack and appear in the actions menu
    private static final int ACTION_NEXT_DATA_POINT = AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD;
    private static final int ACTION_PREVIOUS_DATA_POINT = AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD;

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

        // Add scroll actions for data point navigation if available
        if (!dataPointDescriptions.isEmpty()) {
            // Mark as scrollable so TalkBack shows scroll actions
            info.setScrollable(true);

            // Set collection info to indicate this is a list-like structure
            info.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
                    dataPointDescriptions.size(), 1, false));

            // Add scroll forward action (next data point)
            if (currentDataPointIndex < dataPointDescriptions.size() - 1) {
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
            }

            // Add scroll backward action (previous data point)
            if (currentDataPointIndex > 0) {
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
            }

            // Set state description to show current position
            String stateDesc = getStateDescription();
            if (!stateDesc.isEmpty()) {
                info.setStateDescription(stateDesc);
            }

            // Also set content description to include current data point
            String currentDesc = getCurrentDataPointDescription();
            if (currentDesc != null) {
                CharSequence existingDesc = info.getContentDescription();
                if (existingDesc != null && existingDesc.length() > 0) {
                    info.setContentDescription(existingDesc + ". " + currentDesc);
                }
            }
        }
    }

    @Override
    public boolean performAccessibilityAction(@NonNull View host, int action,
                                               @Nullable Bundle args) {
        switch (action) {
            case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                if (currentDataPointIndex < dataPointDescriptions.size() - 1) {
                    currentDataPointIndex++;
                    announceDataPoint(host);
                    // Notify that the node info has changed so TalkBack updates available actions
                    notifyNodeChanged(host);
                    return true;
                }
                break;

            case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                if (currentDataPointIndex > 0) {
                    currentDataPointIndex--;
                    announceDataPoint(host);
                    // Notify that the node info has changed so TalkBack updates available actions
                    notifyNodeChanged(host);
                    return true;
                }
                break;
        }

        return super.performAccessibilityAction(host, action, args);
    }

    /**
     * Notify accessibility services that the node info has changed.
     * This updates the available actions in TalkBack's menu.
     */
    private void notifyNodeChanged(View host) {
        host.post(() -> {
            host.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        });
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
