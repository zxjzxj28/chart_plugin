package com.yourcompany.a11y.model;

/**
 * Model representing a single chart configuration.
 */
public class ChartConfig {
    private String id;
    private String type;
    private String title;
    private ChartData data;
    private String context;

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

    public ChartData getData() {
        return data;
    }

    public void setData(ChartData data) {
        this.data = data;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
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
                ", data=" + data +
                ", context='" + context + '\'' +
                '}';
    }
}
