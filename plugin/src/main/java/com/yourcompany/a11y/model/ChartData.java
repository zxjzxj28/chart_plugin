package com.yourcompany.a11y.model;

import java.util.List;

/**
 * Model representing chart data containing labels and datasets.
 */
public class ChartData {
    private List<String> labels;
    private List<Dataset> datasets;

    public ChartData() {
    }

    public ChartData(List<String> labels, List<Dataset> datasets) {
        this.labels = labels;
        this.datasets = datasets;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    @Override
    public String toString() {
        return "ChartData{" +
                "labels=" + labels +
                ", datasets=" + datasets +
                '}';
    }
}
