package com.yourcompany.a11y.model;

import java.util.List;

/**
 * Model representing localized accessibility descriptions for a chart.
 */
public class LocalizedDescription {
    private String brief;
    private String detailed;
    private List<String> dataPoints;

    public LocalizedDescription() {
    }

    public LocalizedDescription(String brief, String detailed, List<String> dataPoints) {
        this.brief = brief;
        this.detailed = detailed;
        this.dataPoints = dataPoints;
    }

    public String getBrief() {
        return brief;
    }

    public void setBrief(String brief) {
        this.brief = brief;
    }

    public String getDetailed() {
        return detailed;
    }

    public void setDetailed(String detailed) {
        this.detailed = detailed;
    }

    public List<String> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<String> dataPoints) {
        this.dataPoints = dataPoints;
    }

    @Override
    public String toString() {
        return "LocalizedDescription{" +
                "brief='" + brief + '\'' +
                ", detailed='" + detailed + '\'' +
                ", dataPoints=" + dataPoints +
                '}';
    }
}
