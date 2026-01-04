package com.yourcompany.a11y.model;

/**
 * Model representing a single chart configuration.
 */
public class ChartConfig {
    private String id;
    private String type;
    private String title;
    @com.google.gson.annotations.SerializedName("x_label")
    private String xLabel;
    @com.google.gson.annotations.SerializedName("y_label")
    private String yLabel;
    private java.util.List<ChartPoint> data;

    public ChartConfig() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ChartType getChartType() {
        return ChartType.fromString(type);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getXLabel() {
        return xLabel;
    }

    public void setXLabel(String xLabel) {
        this.xLabel = xLabel;
    }

    public String getYLabel() {
        return yLabel;
    }

    public void setYLabel(String yLabel) {
        this.yLabel = yLabel;
    }

    public java.util.List<ChartPoint> getData() {
        return data;
    }

    public void setData(java.util.List<ChartPoint> data) {
        this.data = data;
    }

    /**
     * Convert chart ID to a valid resource name.
     * - Convert to lowercase
     * - Replace non-alphanumeric characters with underscores
     * - Merge consecutive underscores
     * - Remove leading and trailing underscores
     *
     * @return the sanitized resource name
     */
    public String getResourceName() {
        if (id == null || id.isEmpty()) {
            return "unknown";
        }
        String result = id.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return result.isEmpty() ? "unknown" : result;
    }

    @Override
    public String toString() {
        return "ChartConfig{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", xLabel='" + xLabel + '\'' +
                ", yLabel='" + yLabel + '\'' +
                ", data=" + data +
                '}';
    }
}
