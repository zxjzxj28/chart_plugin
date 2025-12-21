# Chart Accessibility Gradle Plugin (chart-a11y)

A comprehensive Android Gradle plugin and runtime SDK for adding accessibility support to chart components. This plugin generates accessibility descriptions at compile-time by calling a remote AI API, ensuring zero runtime latency.

## Features

- **Compile-time description generation**: Accessibility descriptions are generated during build, not at runtime
- **Multiple chart types**: Supports bar, line, pie, radar, scatter, candlestick, funnel, gauge, heatmap, treemap, and custom charts
- **Multi-language support**: Generate descriptions in multiple locales
- **Two usage modes**: XML-based or code-based accessibility configuration
- **Smart caching**: Avoids redundant API calls with SHA-256 based caching
- **Data point navigation**: Supports screen reader navigation through individual data points

## Requirements

- Java 11+
- Gradle 8.x
- Android Gradle Plugin 8.1.0+
- Minimum Android SDK 21

## Installation

### 1. Add the plugin to your project

```groovy
// settings.gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// app/build.gradle
plugins {
    id 'com.android.application'
    id 'com.yourcompany.chart-a11y' version '1.0.0'
}
```

### 2. Add the runtime dependency

```groovy
dependencies {
    implementation 'com.yourcompany:chart-a11y-runtime:1.0.0'
}
```

### 3. Configure the plugin

```groovy
chartA11y {
    configFile 'src/main/assets/chart_configs.json'  // Chart configuration file
    apiEndpoint = 'https://api.example.com/a11y'     // Remote API endpoint
    apiKey = System.getenv('CHART_A11Y_API_KEY')     // API key (from environment)
    locales 'zh-CN', 'en'                            // Supported languages
    processLayouts = true                             // Process layout XML files
    enableCache = true                                // Enable response caching
    cacheDir = file("${buildDir}/chart-a11y-cache")  // Cache directory
    timeout = 30L                                     // API timeout (seconds)
    concurrency = 4                                   // Concurrent API requests
    failOnError = false                               // Fail build on API errors
}
```

## Usage

### Chart Configuration File

Create a `chart_configs.json` file in your assets directory:

```json
{
  "version": "1.0",
  "charts": [
    {
      "id": "sales_quarterly",
      "type": "BAR",
      "title": "Quarterly Sales",
      "data": {
        "labels": ["Q1", "Q2", "Q3", "Q4"],
        "datasets": [
          {
            "name": "Sales",
            "values": [120, 150, 180, 90],
            "unit": "万元"
          }
        ]
      },
      "context": "Annual sales trend"
    }
  ]
}
```

### Supported Chart Types

- `BAR` - Bar chart
- `LINE` - Line chart
- `PIE` - Pie chart
- `SCATTER` - Scatter chart
- `RADAR` - Radar chart
- `CANDLESTICK` - Candlestick chart
- `FUNNEL` - Funnel chart
- `GAUGE` - Gauge chart
- `HEATMAP` - Heatmap
- `TREEMAP` - Treemap
- `CUSTOM` - Custom chart type

### XML-Based Usage

Add custom attributes to your chart views in layout XML:

```xml
<com.github.mikephil.charting.charts.BarChart
    android:id="@+id/salesChart"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    app:a11yChartId="sales_quarterly"
    app:a11yDescType="detailed"
    app:a11yFocusable="true" />
```

The plugin will automatically inject standard Android accessibility attributes during build.

### Code-Based Usage

#### Simple API

```java
BarChart chart = findViewById(R.id.salesChart);
ChartA11y.apply(chart, "sales_quarterly");

// With description type
ChartA11y.apply(chart, "sales_quarterly", DescType.DETAILED);
```

#### Builder Pattern

```java
ChartA11y.with(chart)
    .chartId("sales_quarterly")
    .descType(DescType.DETAILED)
    .focusable(true)
    .roleDescription("Bar Chart")
    .enableDataPointNavigation(true)
    .apply();
```

## Build Commands

```bash
# Generate accessibility descriptions
./gradlew generateChartA11yDebug

# Process layout files
./gradlew processChartA11yLayoutsDebug

# Full build
./gradlew assembleDebug
```

## Generated Resources

The plugin generates string resources in the format:

```xml
<!-- build/generated/res/a11y/debug/values-zh-rCN/chart_a11y_strings.xml -->
<resources>
    <string name="a11y_chart_sales_quarterly_brief">柱状图显示四个季度销售额</string>
    <string name="a11y_chart_sales_quarterly_detailed">这是一个柱状图...</string>
    <string name="a11y_chart_sales_quarterly_point_0">第一季度，销售额120万元</string>
    ...
</resources>
```

### Resource Naming Convention

- Brief description: `a11y_chart_{chartId}_brief`
- Detailed description: `a11y_chart_{chartId}_detailed`
- Data point: `a11y_chart_{chartId}_point_{index}`

### Locale Directory Mapping

- `zh-CN` → `values-zh-rCN`
- `en` / `en-US` → `values` (default)
- `xx-YY` → `values-xx-rYY`

## API Specification

### Request Format

```json
POST /api/v1/chart-a11y
{
  "chart": {
    "id": "sales_quarterly",
    "type": "BAR",
    "title": "Quarterly Sales",
    "data": {
      "labels": ["Q1", "Q2", "Q3", "Q4"],
      "datasets": [{"name": "Sales", "values": [120, 150, 180, 90], "unit": "万元"}]
    }
  },
  "locales": ["zh-CN", "en"],
  "options": {
    "includeBrief": true,
    "includeDetailed": true,
    "includeDataPoints": true
  }
}
```

### Response Format

```json
{
  "descriptions": {
    "zh-CN": {
      "brief": "柱状图显示...",
      "detailed": "这是一个柱状图...",
      "dataPoints": ["第一季度...", "第二季度..."]
    },
    "en": {
      "brief": "Bar chart showing...",
      "detailed": "This is a bar chart...",
      "dataPoints": ["Q1...", "Q2..."]
    }
  }
}
```

## Project Structure

```
chart-a11y/
├── plugin/                              # Gradle plugin module
│   ├── build.gradle
│   └── src/main/java/com/yourcompany/a11y/
│       ├── ChartA11yPlugin.java         # Plugin entry
│       ├── ChartA11yExtension.java      # DSL extension
│       ├── tasks/
│       │   ├── GenerateDescriptionsTask.java
│       │   └── ProcessLayoutsTask.java
│       ├── model/                       # Data models
│       ├── service/                     # API and cache services
│       └── generator/                   # Resource generators
│
├── runtime/                             # Runtime SDK (AAR)
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/yourcompany/a11y/
│       │   ├── ChartA11y.java           # SDK entry
│       │   ├── DescType.java            # Description type enum
│       │   └── A11yDelegate.java        # Accessibility delegate
│       └── res/values/
│           └── attrs.xml                # Custom attributes
│
└── sample/                              # Sample project
    ├── build.gradle
    └── src/main/
        ├── assets/chart_configs.json
        ├── res/layout/activity_main.xml
        └── java/.../MainActivity.java
```

## License

MIT License

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.
