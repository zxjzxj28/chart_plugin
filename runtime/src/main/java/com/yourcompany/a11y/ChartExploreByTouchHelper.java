package com.yourcompany.a11y;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * ExploreByTouchHelper implementation for chart data point navigation.
 *
 * This class creates virtual accessibility nodes for each data point,
 * enabling TalkBack gesture navigation (single finger swipe) between data points.
 *
 * Key features:
 * - Single finger swipe left/right to navigate between data points
 * - Each data point is a focusable virtual node
 * - Full TalkBack gesture support
 */
public class ChartExploreByTouchHelper extends ExploreByTouchHelper {

    private static final String TAG = "ChartExploreHelper";

    private final View hostView;
    private final String chartId;
    private final String roleDescription;
    private final List<String> dataPointDescriptions;
    private final List<Rect> dataPointBounds;

    // Virtual view ID starts from 1 (0 is reserved for HOST_ID)
    private static final int VIRTUAL_ID_OFFSET = 1;

    public ChartExploreByTouchHelper(@NonNull View hostView,
                                      @NonNull String chartId,
                                      @Nullable String roleDescription) {
        super(hostView);
        this.hostView = hostView;
        this.chartId = chartId;
        this.roleDescription = roleDescription;
        this.dataPointDescriptions = new ArrayList<>();
        this.dataPointBounds = new ArrayList<>();
    }

    /**
     * Load data point descriptions from resources.
     */
    public void loadDataPoints(@NonNull Context context, @NonNull String resourcePrefix) {
        dataPointDescriptions.clear();
        dataPointBounds.clear();

        Resources resources = context.getResources();
        String packageName = context.getPackageName();

        for (int i = 0; i < 100; i++) {
            String resourceName = resourcePrefix + i;
            int resId = resources.getIdentifier(resourceName, "string", packageName);
            if (resId == 0) {
                break;
            }
            dataPointDescriptions.add(resources.getString(resId));
        }

        // Calculate bounds for each data point (evenly distributed)
        calculateDataPointBounds();

        Log.d(TAG, "Loaded " + dataPointDescriptions.size() + " data points for chart: " + chartId);
    }

    /**
     * Set data points directly (for dynamic data or testing).
     */
    public void setDataPoints(@NonNull List<String> descriptions) {
        dataPointDescriptions.clear();
        dataPointDescriptions.addAll(descriptions);
        calculateDataPointBounds();
        Log.d(TAG, "Set " + dataPointDescriptions.size() + " data points for chart: " + chartId);
    }

    /**
     * Calculate bounds for each data point.
     * By default, divides the view width evenly among data points.
     */
    private void calculateDataPointBounds() {
        dataPointBounds.clear();

        if (dataPointDescriptions.isEmpty()) {
            return;
        }

        int viewWidth = hostView.getWidth();
        int viewHeight = hostView.getHeight();

        // If view hasn't been laid out yet, use default size
        if (viewWidth == 0) viewWidth = 800;
        if (viewHeight == 0) viewHeight = 400;

        int count = dataPointDescriptions.size();
        int itemWidth = viewWidth / count;

        for (int i = 0; i < count; i++) {
            Rect bounds = new Rect(
                    i * itemWidth,
                    0,
                    (i + 1) * itemWidth,
                    viewHeight
            );
            dataPointBounds.add(bounds);
        }
    }

    /**
     * Set custom bounds for data points.
     * Useful when data points have specific locations on the chart.
     */
    public void setDataPointBounds(@NonNull List<Rect> bounds) {
        if (bounds.size() != dataPointDescriptions.size()) {
            Log.w(TAG, "Bounds count doesn't match data point count");
            return;
        }
        dataPointBounds.clear();
        dataPointBounds.addAll(bounds);
    }

    public int getDataPointCount() {
        return dataPointDescriptions.size();
    }

    @Override
    protected int getVirtualViewAt(float x, float y) {
        for (int i = 0; i < dataPointBounds.size(); i++) {
            if (dataPointBounds.get(i).contains((int) x, (int) y)) {
                return VIRTUAL_ID_OFFSET + i;
            }
        }
        return ExploreByTouchHelper.HOST_ID;
    }

    @Override
    protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
        for (int i = 0; i < dataPointDescriptions.size(); i++) {
            virtualViewIds.add(VIRTUAL_ID_OFFSET + i);
        }
    }

    @Override
    protected void onPopulateNodeForVirtualView(int virtualViewId,
                                                 @NonNull AccessibilityNodeInfoCompat node) {
        int index = virtualViewId - VIRTUAL_ID_OFFSET;

        if (index < 0 || index >= dataPointDescriptions.size()) {
            // Invalid virtual view ID, provide fallback
            node.setContentDescription("Unknown data point");
            node.setBoundsInParent(new Rect(0, 0, 1, 1));
            return;
        }

        String description = dataPointDescriptions.get(index);
        Rect bounds = dataPointBounds.get(index);

        // Set content description with position info
        String positionInfo = String.format("第 %d 项，共 %d 项",
                index + 1, dataPointDescriptions.size());
        node.setContentDescription(description + "。" + positionInfo);

        // Set bounds
        node.setBoundsInParent(bounds);

        // Make focusable and clickable
        node.setFocusable(true);
        node.setClickable(true);

        // Add click action
        node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);

        // Set role description if available
        if (roleDescription != null && !roleDescription.isEmpty()) {
            node.setRoleDescription("数据点");
        }

        // Set class name
        node.setClassName("android.view.View");

        // Add collection item info for better TalkBack experience
        node.setCollectionItemInfo(
                AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                        index, // row
                        1,     // row span
                        0,     // column
                        1,     // column span
                        false, // heading
                        false  // selected
                )
        );
    }

    @Override
    protected boolean onPerformActionForVirtualView(int virtualViewId,
                                                     int action,
                                                     @Nullable Bundle arguments) {
        int index = virtualViewId - VIRTUAL_ID_OFFSET;

        if (index < 0 || index >= dataPointDescriptions.size()) {
            return false;
        }

        switch (action) {
            case AccessibilityNodeInfoCompat.ACTION_CLICK:
                // Announce the data point when clicked
                String description = dataPointDescriptions.get(index);
                String positionInfo = String.format("第 %d 项，共 %d 项",
                        index + 1, dataPointDescriptions.size());
                hostView.announceForAccessibility(description + "。" + positionInfo);
                return true;
        }

        return false;
    }

    @Override
    protected void onPopulateEventForVirtualView(int virtualViewId,
                                                  @NonNull AccessibilityEvent event) {
        int index = virtualViewId - VIRTUAL_ID_OFFSET;

        if (index >= 0 && index < dataPointDescriptions.size()) {
            event.setContentDescription(dataPointDescriptions.get(index));
        }
    }

    /**
     * Refresh accessibility info after data changes.
     */
    public void refreshAccessibilityInfo() {
        calculateDataPointBounds();
        invalidateRoot();
    }

    /**
     * Focus on a specific data point.
     */
    public void focusDataPoint(int index) {
        if (index >= 0 && index < dataPointDescriptions.size()) {
            int virtualViewId = VIRTUAL_ID_OFFSET + index;
            sendEventForVirtualView(virtualViewId,
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
        }
    }
}
