package com.yourcompany.a11y.model;

import java.util.List;

/**
 * Model representing a dataset within a chart.
 */
public class Dataset {
    private String name;
    private List<Double> values;
    private String unit;
    private String color;

    public Dataset() {
    }

    public Dataset(String name, List<Double> values, String unit) {
        this.name = name;
        this.values = values;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Double> getValues() {
        return values;
    }

    public void setValues(List<Double> values) {
        this.values = values;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "Dataset{" +
                "name='" + name + '\'' +
                ", values=" + values +
                ", unit='" + unit + '\'' +
                '}';
    }
}
