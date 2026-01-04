package com.yourcompany.a11y.model;

/**
 * Model representing a single data point in a chart.
 */
public class ChartPoint {
    @com.google.gson.annotations.SerializedName("x_value")
    private String xValue;
    @com.google.gson.annotations.SerializedName("y_value")
    private Double yValue;
    private String series;

    public ChartPoint() {
    }

    public String getXValue() {
        return xValue;
    }

    public void setXValue(String xValue) {
        this.xValue = xValue;
    }

    public Double getYValue() {
        return yValue;
    }

    public void setYValue(Double yValue) {
        this.yValue = yValue;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    @Override
    public String toString() {
        return "ChartPoint{" +
                "xValue='" + xValue + '\'' +
                ", yValue=" + yValue +
                ", series='" + series + '\'' +
                '}';
    }
}
