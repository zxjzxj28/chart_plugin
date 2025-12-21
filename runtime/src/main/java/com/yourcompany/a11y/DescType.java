package com.yourcompany.a11y;

/**
 * Enumeration of accessibility description types.
 */
public enum DescType {
    /**
     * Brief description providing a quick summary of the chart.
     */
    BRIEF("brief"),

    /**
     * Detailed description providing comprehensive information about the chart.
     */
    DETAILED("detailed");

    private final String suffix;

    DescType(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Get the resource name suffix for this description type.
     *
     * @return the suffix used in resource names
     */
    public String getSuffix() {
        return suffix;
    }
}
