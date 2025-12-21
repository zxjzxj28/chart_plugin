package com.yourcompany.a11y.model;

/**
 * Enumeration of supported chart types for accessibility descriptions.
 */
public enum ChartType {
    BAR("柱状图", "bar chart"),
    LINE("折线图", "line chart"),
    PIE("饼图", "pie chart"),
    SCATTER("散点图", "scatter chart"),
    RADAR("雷达图", "radar chart"),
    CANDLESTICK("K线图", "candlestick chart"),
    FUNNEL("漏斗图", "funnel chart"),
    GAUGE("仪表盘", "gauge chart"),
    HEATMAP("热力图", "heatmap"),
    TREEMAP("矩形树图", "treemap"),
    CUSTOM("自定义图表", "custom chart");

    private final String zhName;
    private final String enName;

    ChartType(String zhName, String enName) {
        this.zhName = zhName;
        this.enName = enName;
    }

    public String getZhName() {
        return zhName;
    }

    public String getEnName() {
        return enName;
    }

    /**
     * Get localized chart type name.
     *
     * @param locale the locale code (e.g., "zh-CN", "en")
     * @return the localized name
     */
    public String getLocalizedName(String locale) {
        if (locale != null && locale.startsWith("zh")) {
            return zhName;
        }
        return enName;
    }

    /**
     * Parse chart type from string, case-insensitive.
     *
     * @param value the string value
     * @return the ChartType, or CUSTOM if not found
     */
    public static ChartType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return CUSTOM;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }
}
