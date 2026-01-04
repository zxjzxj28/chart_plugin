// A11yDelegate.java - 修复版

package com.yourcompany.a11y;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import java.util.ArrayList;
import java.util.List;

public class A11yDelegate extends AccessibilityDelegateCompat {

    private static final String TAG = "A11yDelegate";

    // 使用自定义动作 ID（必须大于 0x10000000）
    private static final int ACTION_ID_NEXT = 0x10000001;
    private static final int ACTION_ID_PREVIOUS = 0x10000002;

    private final String chartId;
    private final String roleDescription;
    private final List<String> dataPointDescriptions;
    private int currentDataPointIndex = 0;

    // 自定义动作标签（可本地化）
    private String actionLabelNext = "下一个数据点";
    private String actionLabelPrevious = "上一个数据点";

    public A11yDelegate(@NonNull String chartId, @Nullable String roleDescription) {
        this.chartId = chartId;
        this.roleDescription = roleDescription;
        this.dataPointDescriptions = new ArrayList<>();
    }

    /**
     * 设置动作标签（支持本地化）
     */
    public void setActionLabels(@NonNull String next, @NonNull String previous) {
        this.actionLabelNext = next;
        this.actionLabelPrevious = previous;
    }

    public void loadDataPoints(@NonNull Context context, @NonNull String resourcePrefix) {
        dataPointDescriptions.clear();
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

        // 调试日志
        Log.d(TAG, "Loaded " + dataPointDescriptions.size() + " data points for chart: " + chartId);
    }

    /**
     * 直接设置数据点描述（用于动态数据或测试）
     */
    public void setDataPoints(@NonNull List<String> descriptions) {
        dataPointDescriptions.clear();
        dataPointDescriptions.addAll(descriptions);
        currentDataPointIndex = 0;
        Log.d(TAG, "Set " + dataPointDescriptions.size() + " data points for chart: " + chartId);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                  @NonNull AccessibilityNodeInfoCompat info) {
        super.onInitializeAccessibilityNodeInfo(host, info);

        // 设置角色描述
        if (roleDescription != null && !roleDescription.isEmpty()) {
            info.setRoleDescription(roleDescription);
        }

        // 关键修复：添加自定义动作
        if (!dataPointDescriptions.isEmpty()) {
            // 添加"下一个数据点"动作
            if (currentDataPointIndex < dataPointDescriptions.size() - 1) {
                AccessibilityNodeInfoCompat.AccessibilityActionCompat nextAction =
                        new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                ACTION_ID_NEXT, actionLabelNext);
                info.addAction(nextAction);
            }

            // 添加"上一个数据点"动作
            if (currentDataPointIndex > 0) {
                AccessibilityNodeInfoCompat.AccessibilityActionCompat prevAction =
                        new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                ACTION_ID_PREVIOUS, actionLabelPrevious);
                info.addAction(prevAction);
            }

            // 同时保留标准滚动动作（双指滑动支持）
            info.setScrollable(true);
            if (currentDataPointIndex < dataPointDescriptions.size() - 1) {
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
            }
            if (currentDataPointIndex > 0) {
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
            }

            // 设置状态描述
            String stateDesc = getStateDescription();
            info.setStateDescription(stateDesc);

            // 设置 CollectionInfo（帮助 TalkBack 理解这是列表结构）
            info.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
                    dataPointDescriptions.size(), 1, false,
                    AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_SINGLE));

            Log.d(TAG, "Node info initialized: " + dataPointDescriptions.size() +
                    " points, current=" + currentDataPointIndex);
        }
    }

    @Override
    public boolean performAccessibilityAction(@NonNull View host, int action,
                                              @Nullable Bundle args) {
        Log.d(TAG, "performAccessibilityAction: action=" + action);

        switch (action) {
            case ACTION_ID_NEXT:
            case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                if (currentDataPointIndex < dataPointDescriptions.size() - 1) {
                    currentDataPointIndex++;
                    announceDataPoint(host);
                    notifyNodeChanged(host);
                    return true;
                }
                return false;

            case ACTION_ID_PREVIOUS:
            case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                if (currentDataPointIndex > 0) {
                    currentDataPointIndex--;
                    announceDataPoint(host);
                    notifyNodeChanged(host);
                    return true;
                }
                return false;
        }

        return super.performAccessibilityAction(host, action, args);
    }

    private void notifyNodeChanged(View host) {
        // 通知节点信息已变化
        host.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);

        // 重新获取焦点以触发状态更新
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            host.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
        }
    }

    private void announceDataPoint(View host) {
        String description = getCurrentDataPointDescription();
        if (description != null) {
            String announcement = description + "。" + getStateDescription();
            host.announceForAccessibility(announcement);
        }
    }

    public String getStateDescription() {
        if (dataPointDescriptions.isEmpty()) {
            return "";
        }
        return String.format("第 %d 项，共 %d 项",
                currentDataPointIndex + 1, dataPointDescriptions.size());
    }

    @Nullable
    public String getCurrentDataPointDescription() {
        if (dataPointDescriptions.isEmpty() ||
                currentDataPointIndex >= dataPointDescriptions.size()) {
            return null;
        }
        return dataPointDescriptions.get(currentDataPointIndex);
    }

    public void navigateToDataPoint(int index) {
        if (index >= 0 && index < dataPointDescriptions.size()) {
            currentDataPointIndex = index;
        }
    }

    public void resetNavigation() {
        currentDataPointIndex = 0;
    }

    public int getDataPointCount() {
        return dataPointDescriptions.size();
    }

    public int getCurrentIndex() {
        return currentDataPointIndex;
    }
}
