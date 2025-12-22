package com.yourcompany.a11y.sample;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.yourcompany.a11y.ChartA11y;
import com.yourcompany.a11y.DescType;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample activity demonstrating Chart Accessibility features.
 *
 * This activity shows four different charts:
 * 1. Bar Chart - accessibility configured via XML attributes (with data point navigation)
 * 2. Line Chart - accessibility configured via code (simple API)
 * 3. Pie Chart - accessibility configured via XML attributes (with data point navigation)
 * 4. Radar Chart - accessibility configured via code (Builder pattern)
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize data point navigation for all XML-configured charts
        // This enables swipe navigation for charts with app:a11yEnableNavigation="true"
        int initializedCount = ChartA11y.initializeFromXml(this);
        android.util.Log.d("MainActivity", "Initialized " + initializedCount + " charts from XML");

        setupBarChart();
        setupLineChart();
        setupPieChart();
        setupRadarChart();
    }

    /**
     * Set up the bar chart with sample data.
     * Accessibility is configured via XML attributes (a11yChartId, a11yDescType).
     */
    private void setupBarChart() {
        BarChart chart = findViewById(R.id.barChart);

        // Sample data: Quarterly sales
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 120));
        entries.add(new BarEntry(1, 150));
        entries.add(new BarEntry(2, 180));
        entries.add(new BarEntry(3, 90));

        BarDataSet dataSet = new BarDataSet(entries, "Sales (万元)");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        BarData data = new BarData(dataSet);
        chart.setData(data);

        // Configure X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Q1", "Q2", "Q3", "Q4"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        chart.getDescription().setEnabled(false);
        chart.animateY(1000);
        chart.invalidate();
    }

    /**
     * Set up the line chart with sample data.
     * Accessibility is configured via code using the simple API.
     */
    private void setupLineChart() {
        LineChart chart = findViewById(R.id.lineChart);

        // Sample data: User growth
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 1200));
        entries.add(new Entry(1, 1500));
        entries.add(new Entry(2, 1800));
        entries.add(new Entry(3, 2200));
        entries.add(new Entry(4, 2800));
        entries.add(new Entry(5, 3500));

        LineDataSet dataSet = new LineDataSet(entries, "New Users");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);

        LineData data = new LineData(dataSet);
        chart.setData(data);

        // Configure X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        chart.getDescription().setEnabled(false);
        chart.animateX(1000);
        chart.invalidate();

        // Apply accessibility using simple API
        ChartA11y.apply(chart, "user_growth", DescType.DETAILED);
    }

    /**
     * Set up the pie chart with sample data.
     * Accessibility is configured via XML attributes.
     */
    private void setupPieChart() {
        PieChart chart = findViewById(R.id.pieChart);

        // Sample data: Market share
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(35f, "Product A"));
        entries.add(new PieEntry(28f, "Product B"));
        entries.add(new PieEntry(22f, "Product C"));
        entries.add(new PieEntry(15f, "Others"));

        PieDataSet dataSet = new PieDataSet(entries, "Market Share");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        chart.setData(data);

        chart.getDescription().setEnabled(false);
        chart.setEntryLabelColor(Color.WHITE);
        chart.animateY(1000);
        chart.invalidate();
    }

    /**
     * Set up the radar chart with sample data.
     * Accessibility is configured via code using the Builder pattern.
     */
    private void setupRadarChart() {
        RadarChart chart = findViewById(R.id.radarChart);

        // Sample data: Performance metrics
        List<RadarEntry> entries = new ArrayList<>();
        entries.add(new RadarEntry(85f));
        entries.add(new RadarEntry(90f));
        entries.add(new RadarEntry(78f));
        entries.add(new RadarEntry(82f));
        entries.add(new RadarEntry(88f));

        RadarDataSet dataSet = new RadarDataSet(entries, "Performance");
        dataSet.setColor(Color.rgb(103, 110, 129));
        dataSet.setFillColor(Color.rgb(103, 110, 129));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(180);
        dataSet.setLineWidth(2f);
        dataSet.setDrawHighlightCircleEnabled(true);

        RadarData data = new RadarData(dataSet);
        chart.setData(data);

        // Configure X axis (labels)
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Communication", "Technical", "Teamwork", "Innovation", "Execution"}));

        chart.getDescription().setEnabled(false);
        chart.animateXY(1000, 1000);
        chart.invalidate();

        // Apply accessibility using Builder pattern
        ChartA11y.with(chart)
                .chartId("performance_radar")
                .descType(DescType.DETAILED)
                .focusable(true)
                .roleDescription("Radar Chart")
                .enableDataPointNavigation(true)
                .apply();
    }
}
