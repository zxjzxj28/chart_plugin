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
    private int currentDataPointIndex = -1;  // 初始化为-1，表示未选中任何数据点

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
        currentDataPointIndex = -1;  // 重置为未选中状态
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
            // 不覆盖 contentDescription，保留图表摘要供首次聚焦时播报
            // 数据点描述仅在导航操作时通过 announceForAccessibility 播报

            // 添加"下一个数据点"动作
            // 如果未选中任何数据点(index=-1)或未到最后一个，允许前进
            if (currentDataPointIndex < dataPointDescriptions.size() - 1) {
                AccessibilityNodeInfoCompat.AccessibilityActionCompat nextAction =
                        new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                ACTION_ID_NEXT, actionLabelNext);
                info.addAction(nextAction);
            }

            // 添加"上一个数据点"动作
            // 只有当已选中数据点且不是第一个时，才允许后退
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

            // 设置状态描述 - 使用简洁格式，避免被读作"列表"
            String stateDesc = getStateDescription();
            info.setStateDescription(stateDesc);

            // 移除 CollectionInfo 设置以避免 TalkBack 播报"列表"

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
        // 只通知节点信息已变化，不重新触发焦点事件
        // 避免重新播报 contentDescription（图表摘要）覆盖数据点描述
        host.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }

    private void announceDataPoint(View host) {
        String description = getCurrentDataPointDescription();
        if (description != null) {
            // 只播报数据点描述，不附加状态信息
            // 状态信息已在 stateDescription 中设置，TalkBack 会在合适的时候读取
            host.announceForAccessibility(description);
        }
    }

    public String getStateDescription() {
        if (dataPointDescriptions.isEmpty() || currentDataPointIndex < 0) {
            return "";
        }
        // 使用更简洁的格式，避免"项"字可能被读作"列表"
        return String.format("%d / %d",
                currentDataPointIndex + 1, dataPointDescriptions.size());
    }

    @Nullable
    public String getCurrentDataPointDescription() {
        if (dataPointDescriptions.isEmpty() ||
                currentDataPointIndex < 0 ||
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
        currentDataPointIndex = -1;  // 重置为未选中状态
    }

    public int getDataPointCount() {
        return dataPointDescriptions.size();
    }

    public int getCurrentIndex() {
        return currentDataPointIndex;
    }
}
