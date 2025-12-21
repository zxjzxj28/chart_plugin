package com.yourcompany.a11y.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing the entire chart configuration file.
 */
public class ChartConfigFile {
    private String version;
    private List<ChartConfig> charts;

    public ChartConfigFile() {
        this.charts = new ArrayList<>();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ChartConfig> getCharts() {
        return charts;
    }

    public void setCharts(List<ChartConfig> charts) {
        this.charts = charts;
    }

    /**
     * Find a chart configuration by ID.
     *
     * @param id the chart ID
     * @return the chart configuration, or null if not found
     */
    public ChartConfig findById(String id) {
        if (charts == null || id == null) {
            return null;
        }
        for (ChartConfig chart : charts) {
            if (id.equals(chart.getId())) {
                return chart;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ChartConfigFile{" +
                "version='" + version + '\'' +
                ", charts=" + charts +
                '}';
    }
}
