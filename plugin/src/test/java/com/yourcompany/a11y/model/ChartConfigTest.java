package com.yourcompany.a11y.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ChartConfig model.
 */
public class ChartConfigTest {

    @Test
    public void testGetResourceName_normalId() {
        ChartConfig config = new ChartConfig();
        config.setId("sales_quarterly");
        assertEquals("sales_quarterly", config.getResourceName());
    }

    @Test
    public void testGetResourceName_withUpperCase() {
        ChartConfig config = new ChartConfig();
        config.setId("Sales_Quarterly");
        assertEquals("sales_quarterly", config.getResourceName());
    }

    @Test
    public void testGetResourceName_withSpecialChars() {
        ChartConfig config = new ChartConfig();
        config.setId("sales-quarterly@2024");
        assertEquals("sales_quarterly_2024", config.getResourceName());
    }

    @Test
    public void testGetResourceName_withConsecutiveUnderscores() {
        ChartConfig config = new ChartConfig();
        config.setId("sales___quarterly");
        assertEquals("sales_quarterly", config.getResourceName());
    }

    @Test
    public void testGetResourceName_withLeadingTrailingUnderscores() {
        ChartConfig config = new ChartConfig();
        config.setId("_sales_quarterly_");
        assertEquals("sales_quarterly", config.getResourceName());
    }

    @Test
    public void testGetResourceName_nullId() {
        ChartConfig config = new ChartConfig();
        config.setId(null);
        assertEquals("unknown", config.getResourceName());
    }

    @Test
    public void testGetResourceName_emptyId() {
        ChartConfig config = new ChartConfig();
        config.setId("");
        assertEquals("unknown", config.getResourceName());
    }

    @Test
    public void testGetChartType_validType() {
        ChartConfig config = new ChartConfig();
        config.setType("BAR");
        assertEquals(ChartType.BAR, config.getChartType());
    }

    @Test
    public void testGetChartType_lowercaseType() {
        ChartConfig config = new ChartConfig();
        config.setType("bar");
        assertEquals(ChartType.BAR, config.getChartType());
    }

    @Test
    public void testGetChartType_invalidType() {
        ChartConfig config = new ChartConfig();
        config.setType("UNKNOWN_TYPE");
        assertEquals(ChartType.CUSTOM, config.getChartType());
    }

    @Test
    public void testGetChartType_nullType() {
        ChartConfig config = new ChartConfig();
        config.setType(null);
        assertEquals(ChartType.CUSTOM, config.getChartType());
    }
}
