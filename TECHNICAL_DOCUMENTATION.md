# Chart Accessibility Gradle Plugin 技术文档

> **版本**: 1.0.0
> **项目名称**: chart-a11y
> **文档更新日期**: 2024年

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术架构](#2-技术架构)
3. [Plugin 模块详解](#3-plugin-模块详解)
4. [Runtime 模块详解](#4-runtime-模块详解)
5. [Sample 模块详解](#5-sample-模块详解)
6. [数据流程图](#6-数据流程图)
7. [技术栈总结](#7-技术栈总结)

---

## 1. 项目概述

### 1.1 项目简介

Chart Accessibility Gradle Plugin (chart-a11y) 是一个为 Android 图表组件提供无障碍功能的完整解决方案。该项目通过 **编译时生成** 的方式，利用远程 AI API 为图表生成多语言无障碍描述，并在运行时提供丰富的无障碍交互支持。

### 1.2 核心功能

| 功能 | 描述 |
|------|------|
| **编译时描述生成** | 在 Android 编译过程中自动调用 AI API 生成图表无障碍描述 |
| **多语言支持** | 支持生成多个语言版本的无障碍描述（如 zh-CN、en） |
| **智能缓存** | 基于 SHA-256 哈希的 API 响应缓存，避免重复调用 |
| **XML 布局处理** | 自动处理布局文件中的自定义属性，注入标准无障碍属性 |
| **运行时 SDK** | 提供灵活的 API 和 Builder 模式应用无障碍功能 |
| **数据点导航** | 支持屏幕阅读器逐个数据点导航 |

### 1.3 项目结构

```
chart_plugin/
├── plugin/                  # Gradle 插件模块
│   ├── src/main/java/      # 插件源代码
│   └── src/test/java/      # 单元测试
├── runtime/                 # 运行时 SDK 模块 (AAR)
│   ├── src/main/java/      # SDK 源代码
│   └── src/main/res/       # 自定义属性定义
├── sample/                  # 示例应用模块
│   ├── src/main/java/      # 示例代码
│   └── src/main/assets/    # 图表配置文件
├── build.gradle            # 根项目构建配置
└── settings.gradle         # 项目设置
```

---

## 2. 技术架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        编译时 (Build Time)                        │
├─────────────────────────────────────────────────────────────────┤
│  chart_configs.json                                              │
│         │                                                        │
│         ▼                                                        │
│  ┌─────────────────┐    ┌─────────────────┐   ┌───────────────┐ │
│  │ GenerateDesc    │───▶│ A11yApiService  │──▶│ Remote AI API │ │
│  │ Task            │    └─────────────────┘   └───────────────┘ │
│  └────────┬────────┘             │                              │
│           │              ┌───────▼───────┐                       │
│           │              │ CacheService  │                       │
│           │              └───────────────┘                       │
│           ▼                                                      │
│  ┌─────────────────┐                                            │
│  │ StringResource  │───▶ build/generated/res/a11y/values/       │
│  │ Generator       │     chart_a11y_strings.xml                 │
│  └─────────────────┘                                            │
│                                                                  │
│  ┌─────────────────┐                                            │
│  │ ProcessLayouts  │───▶ build/generated/res/a11y-layouts/      │
│  │ Task            │     (处理后的布局文件)                       │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        运行时 (Runtime)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐    ┌──────────────┐    ┌──────────────────┐    │
│  │ ChartA11y   │───▶│ A11yDelegate │───▶│ TalkBack/无障碍   │    │
│  │ SDK         │    │              │    │ 服务             │    │
│  └─────────────┘    └──────────────┘    └──────────────────┘    │
│         │                                                        │
│         ▼                                                        │
│  ┌─────────────────────────────────────┐                        │
│  │ R.string.a11y_chart_xxx_brief       │                        │
│  │ R.string.a11y_chart_xxx_detailed    │                        │
│  │ R.string.a11y_chart_xxx_point_N     │                        │
│  └─────────────────────────────────────┘                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 核心技术栈

| 类别 | 技术/框架 | 版本 | 用途 |
|------|----------|------|------|
| **构建系统** | Gradle | 8.x | 项目构建和任务管理 |
| **Android 工具链** | Android Gradle Plugin (AGP) | 8.1.0 | Android 项目构建支持 |
| **编程语言** | Java | 11+ | 主要开发语言 |
| **网络请求** | OkHttp | 4.11.0 | HTTP 客户端，API 调用 |
| **JSON 处理** | Gson | 2.10.1 | JSON 序列化/反序列化 |
| **Android SDK** | Android SDK | 21-34 | 目标平台 |
| **AndroidX** | Core, Annotations | 1.12.0, 1.7.0 | Android 兼容库 |
| **测试框架** | JUnit | 4.13.2 | 单元测试 |
| **Mock 服务器** | OkHttp MockWebServer | 4.11.0 | API 测试 |

---

## 3. Plugin 模块详解

### 3.1 模块概述

**路径**: `/plugin/`
**类型**: Gradle 插件 (java-gradle-plugin)
**包名**: `com.yourcompany.a11y`
**发布 ID**: `com.yourcompany.chart-a11y`

Plugin 模块是整个项目的核心，负责在 Android 编译过程中：
1. 读取图表配置文件
2. 调用远程 AI API 生成无障碍描述
3. 管理 API 响应缓存
4. 生成 Android 字符串资源文件
5. 处理布局 XML 文件

### 3.2 核心类详解

#### 3.2.1 ChartA11yPlugin.java

**文件路径**: `plugin/src/main/java/com/yourcompany/a11y/ChartA11yPlugin.java`

**实现原理**:

```java
public class ChartA11yPlugin implements Plugin<Project>
```

这是 Gradle 插件的入口点，实现了 `org.gradle.api.Plugin<Project>` 接口。

**核心功能**:

| 功能 | 实现方式 |
|------|----------|
| **创建 DSL 扩展** | 使用 `project.getExtensions().create()` 创建 `chartA11y {}` 配置块 |
| **监听 Android 插件** | 使用 `project.getPlugins().withId()` 监听 `com.android.application` 和 `com.android.library` |
| **获取 Android Variants** | 使用 AGP 的 `AndroidComponentsExtension` 获取所有构建变体 |
| **注册任务** | 为每个 Variant 注册 `generateChartA11y{Variant}` 和 `processChartA11yLayouts{Variant}` 任务 |
| **添加生成资源** | 使用 `variant.getSources().getRes().addGeneratedSourceDirectory()` 添加生成的资源目录 |

**关键技术**:

1. **Variant-aware 任务注册**:
   - 使用 `androidComponents.onVariants()` 遍历所有变体
   - 每个变体（如 debug、release）都会生成独立的任务

2. **任务依赖管理**:
   - 确保 `generateChartA11y` 在 `generate{Variant}Resources` 之前执行
   - 使用 `mustRunAfter` 确保正确的执行顺序

```java
// 任务依赖示例
project.afterEvaluate(p -> {
    String generateResourcesTaskName = "generate" + variantName + "Resources";
    Task generateResourcesTask = p.getTasks().findByName(generateResourcesTaskName);
    if (generateResourcesTask != null) {
        generateResourcesTask.dependsOn(generateTask);
    }
});
```

---

#### 3.2.2 ChartA11yExtension.java

**文件路径**: `plugin/src/main/java/com/yourcompany/a11y/ChartA11yExtension.java`

**实现原理**:

```java
public abstract class ChartA11yExtension
```

这是一个抽象类，使用 Gradle 的 **Managed Properties** 特性，无需手动实现 getter/setter。

**DSL 配置示例**:

```groovy
chartA11y {
    configFile 'src/main/assets/chart_configs.json'
    apiEndpoint = 'https://api.example.com/a11y'
    apiKey = 'your-api-key'
    locales 'zh-CN', 'en'
    processLayouts = true
    enableCache = true
    cacheDir = file("${buildDir}/chart-a11y-cache")
    timeout = 30L
    concurrency = 4
    failOnError = false
}
```

**配置属性详解**:

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `configFile` | String | - | 图表配置 JSON 文件路径 |
| `apiEndpoint` | String | `https://api.example.com/a11y` | AI API 端点 |
| `apiKey` | String | `""` | API 认证密钥 |
| `locales` | List<String> | - | 支持的语言列表 |
| `processLayouts` | Boolean | `true` | 是否处理布局文件 |
| `enableCache` | Boolean | `true` | 是否启用缓存 |
| `cacheDir` | File | `${buildDir}/chart-a11y-cache` | 缓存目录 |
| `timeout` | Long | `30` | API 超时时间（秒） |
| `concurrency` | Integer | `4` | 并发请求数 |
| `failOnError` | Boolean | `false` | API 失败时是否终止构建 |

**关键技术**:

1. **Gradle Managed Properties**:
   ```java
   // 抽象方法自动由 Gradle 实现
   public abstract Property<String> getApiEndpoint();
   public abstract ListProperty<String> getLocales();
   ```

2. **Convention（默认值）设置**:
   ```java
   getApiEndpoint().convention("https://api.example.com/a11y");
   getEnableCache().convention(true);
   getTimeout().convention(30L);
   ```

3. **Groovy DSL 便捷方法**:
   ```java
   public void locales(String... locales) {
       getLocales().set(java.util.Arrays.asList(locales));
   }
   ```

---

#### 3.2.3 GenerateDescriptionsTask.java

**文件路径**: `plugin/src/main/java/com/yourcompany/a11y/tasks/GenerateDescriptionsTask.java`

**实现原理**:

```java
public abstract class GenerateDescriptionsTask extends DefaultTask
```

这是核心的 Gradle 任务，负责生成无障碍描述资源。

**执行流程**:

```
┌──────────────────────────────────────────────────────────────────┐
│                    GenerateDescriptionsTask                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. 读取 chart_configs.json                                       │
│         │                                                         │
│         ▼                                                         │
│  2. 解析为 ChartConfigFile 对象                                   │
│         │                                                         │
│         ▼                                                         │
│  3. 遍历每个 ChartConfig                                          │
│         │                                                         │
│         ├──▶ 检查缓存 (CacheService.get)                          │
│         │         │                                               │
│         │    ┌────┴────┐                                          │
│         │    │缓存命中  │──▶ 使用缓存结果                          │
│         │    └─────────┘                                          │
│         │                                                         │
│         └──▶ 缓存未命中                                           │
│                   │                                               │
│                   ▼                                               │
│  4. 批量调用 API (A11yApiService.fetchDescriptionsBatch)          │
│         │                                                         │
│         ▼                                                         │
│  5. 保存到缓存 (CacheService.put)                                 │
│         │                                                         │
│         ▼                                                         │
│  6. 生成字符串资源 (StringResourceGenerator.generate)             │
│         │                                                         │
│         ▼                                                         │
│  build/generated/res/a11y/{variant}/values/chart_a11y_strings.xml │
│  build/generated/res/a11y/{variant}/values-zh-rCN/...             │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

**关键技术**:

1. **增量构建支持**: 使用 Gradle 的 `@InputFile`, `@Input`, `@OutputDirectory` 注解实现增量构建

   ```java
   @InputFile
   public abstract RegularFileProperty getConfigFile();

   @OutputDirectory
   public abstract DirectoryProperty getOutputDir();
   ```

2. **缓存优先策略**: 先检查缓存，避免不必要的 API 调用

   ```java
   for (ChartConfig chart : charts) {
       A11yResult cached = cacheService.get(chart);
       if (cached != null) {
           results.put(chart.getId(), cached);
       } else {
           uncachedCharts.add(chart);
       }
   }
   ```

3. **批量 API 调用**: 使用并发执行提高效率

   ```java
   Map<String, A11yResult> apiResults = apiService.fetchDescriptionsBatch(uncachedCharts);
   ```

---

#### 3.2.4 ProcessLayoutsTask.java

**文件路径**: `plugin/src/main/java/com/yourcompany/a11y/tasks/ProcessLayoutsTask.java`

**实现原理**:

该任务扫描布局 XML 文件，找到包含自定义无障碍属性的元素，并将其转换为标准 Android 无障碍属性。

**处理流程**:

```
┌─────────────────────────────────────────────────────────────────┐
│                     ProcessLayoutsTask                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  输入: src/main/res/layout/*.xml                                 │
│                                                                  │
│  1. 遍历所有 layout* 目录 (layout, layout-land, etc.)            │
│         │                                                        │
│         ▼                                                        │
│  2. 解析每个 XML 文件 (DOM Parser)                               │
│         │                                                        │
│         ▼                                                        │
│  3. 查找包含 app:a11yChartId 属性的元素                          │
│         │                                                        │
│         ▼                                                        │
│  4. 处理每个元素:                                                │
│     ┌──────────────────────────────────────────────────────────┐ │
│     │ 输入属性:                        │ 输出属性:              │ │
│     │ app:a11yChartId="sales_quarterly"│ android:contentDescription │
│     │ app:a11yDescType="detailed"      │   ="@string/a11y_chart_sales_quarterly_detailed" │
│     │ app:a11yFocusable="true"         │ android:focusable="true" │
│     │                                  │ android:importantForAccessibility="yes" │
│     └──────────────────────────────────────────────────────────┘ │
│         │                                                        │
│         ▼                                                        │
│  5. 移除自定义属性                                               │
│         │                                                        │
│         ▼                                                        │
│  输出: build/generated/res/a11y-layouts/{variant}/layout/*.xml   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**关键技术**:

1. **XML DOM 解析**: 使用标准 Java XML API

   ```java
   DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
   factory.setNamespaceAware(true);
   DocumentBuilder builder = factory.newDocumentBuilder();
   Document doc = builder.parse(xmlFile);
   ```

2. **递归元素查找**: 遍历 DOM 树查找目标元素

   ```java
   private void findElementsRecursive(Element element, List<Element> result) {
       if (hasA11yAttribute(element)) {
           result.add(element);
       }
       NodeList children = element.getChildNodes();
       for (int i = 0; i < children.getLength(); i++) {
           Node child = children.item(i);
           if (child instanceof Element) {
               findElementsRecursive((Element) child, result);
           }
       }
   }
   ```

3. **命名空间处理**: 正确处理 Android 和 App 命名空间

   ```java
   private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
   private static final String APP_NS = "http://schemas.android.com/apk/res-auto";

   element.setAttributeNS(ANDROID_NS, "android:contentDescription", contentDescRef);
   ```

4. **资源名称转换**: 将 chartId 转换为合法的资源名称

   ```java
   private String chartIdToResourceName(String chartId) {
       return chartId.toLowerCase()
               .replaceAll("[^a-z0-9]", "_")
               .replaceAll("_+", "_")
               .replaceAll("^_|_$", "");
   }
   ```

---

#### 3.2.5 A11yApiService.java

**文件路径**: `plugin/src/main/java/com/yourcompany/a11y/service/A11yApiService.java`

**实现原理**:

负责与远程 AI API 通信，获取图表无障碍描述。

**核心特性**:

| 特性 | 实现方式 |
|------|----------|
| **HTTP 客户端** | 使用 OkHttp 4.x |
| **并发请求** | 使用 `ExecutorService` 线程池 |
| **重试机制** | 指数退避重试（最多 3 次） |
| **超时控制** | 可配置的连接/读取/写入超时 |

**请求格式**:

```json
{
  "chart": {
    "id": "sales_quarterly",
    "type": "BAR",
    "title": "季度销售额",
    "data": {
      "labels": ["Q1", "Q2", "Q3", "Q4"],
      "datasets": [{"name": "销售额", "values": [120, 150, 180, 90], "unit": "万元"}]
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

**关键技术**:

1. **OkHttp 配置**:

   ```java
   this.client = new OkHttpClient.Builder()
           .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
           .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
           .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
           .build();
   ```

2. **指数退避重试**:

   ```java
   for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
       try {
           if (attempt > 0) {
               // 指数退避: 1s, 2s, 4s
               long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
               Thread.sleep(backoffMs);
           }
           return executeRequest(requestBody);
       } catch (IOException e) {
           lastException = e;
       }
   }
   ```

3. **并发批量请求**:

   ```java
   public Map<String, A11yResult> fetchDescriptionsBatch(List<ChartConfig> configs) {
       Map<String, A11yResult> results = new HashMap<>();
       List<Future<Map.Entry<String, A11yResult>>> futures = new ArrayList<>();

       for (ChartConfig config : configs) {
           Callable<Map.Entry<String, A11yResult>> task = () -> {
               A11yResult result = fetchDescription(config);
               return Map.entry(config.getId(), result);
           };
           futures.add(executor.submit(task));
       }

       // 收集结果
       for (Future<Map.Entry<String, A11yResult>> future : futures) {
           Map.Entry<String, A11yResult> entry = future.get();
           results.put(entry.getKey(), entry.getValue());
       }
       return results;
   }
   ```

---

#### 3.2.6 CacheService.java

**文件路径**: `plugin/src/main/java/com/yourcompany/a11y/service/CacheService.java`

**实现原理**:

基于文件系统的缓存服务，使用 SHA-256 哈希作为缓存键。

**缓存策略**:

```
┌─────────────────────────────────────────────────────────────────┐
│                       缓存工作流程                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ChartConfig JSON ──▶ SHA-256 Hash ──▶ {hash}.json              │
│                                                                  │
│  缓存目录: build/chart-a11y-cache/                               │
│     ├── a1b2c3d4e5f6...json                                     │
│     ├── f6e5d4c3b2a1...json                                     │
│     └── ...                                                      │
│                                                                  │
│  缓存文件结构:                                                   │
│  {                                                               │
│    "dataHash": "a1b2c3d4...",                                   │
│    "result": { ... A11yResult ... },                            │
│    "cachedAt": 1703123456789                                    │
│  }                                                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**关键技术**:

1. **SHA-256 哈希计算**:

   ```java
   public String computeHash(ChartConfig config) {
       MessageDigest digest = MessageDigest.getInstance("SHA-256");
       String json = gson.toJson(config);
       byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
       return bytesToHex(hashBytes);
   }

   private String bytesToHex(byte[] bytes) {
       StringBuilder sb = new StringBuilder();
       for (byte b : bytes) {
           sb.append(String.format("%02x", b));
       }
       return sb.toString();
   }
   ```

2. **缓存读取**:

   ```java
   public A11yResult get(ChartConfig config) {
       if (!enabled) return null;

       String hash = computeHash(config);
       File cacheFile = new File(cacheDir, hash + ".json");

       if (!cacheFile.exists()) return null;

       try (FileReader reader = new FileReader(cacheFile, StandardCharsets.UTF_8)) {
           CacheEntry entry = gson.fromJson(reader, CacheEntry.class);
           return entry != null ? entry.result : null;
       }
   }
   ```

---

#### 3.2.7 StringResourceGenerator.java

**文件路径**: `plugin/src/main/java/com/yourcompany/a11y/generator/StringResourceGenerator.java`

**实现原理**:

生成 Android 字符串资源 XML 文件。

**输出文件结构**:

```
build/generated/res/a11y/{variant}/
├── values/                           # 默认语言 (en)
│   └── chart_a11y_strings.xml
├── values-zh-rCN/                    # 中文
│   └── chart_a11y_strings.xml
└── values-xx-rYY/                    # 其他语言
    └── chart_a11y_strings.xml
```

**生成的 XML 格式**:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Auto-generated by Chart A11y Plugin. Do not edit. -->
    <string name="a11y_chart_sales_quarterly_brief">季度销售柱状图，显示Q1至Q4销售额</string>
    <string name="a11y_chart_sales_quarterly_detailed">这是一个显示年度四个季度销售额的柱状图。Q1销售120万元，Q2销售150万元，Q3销售180万元达到峰值，Q4销售90万元。整体呈先升后降趋势。</string>
    <string name="a11y_chart_sales_quarterly_point_0">第一季度，销售额120万元</string>
    <string name="a11y_chart_sales_quarterly_point_1">第二季度，销售额150万元</string>
    <string name="a11y_chart_sales_quarterly_point_2">第三季度，销售额180万元，最高点</string>
    <string name="a11y_chart_sales_quarterly_point_3">第四季度，销售额90万元</string>
</resources>
```

**关键技术**:

1. **语言目录映射**:

   ```java
   private File getValuesDir(File outputDir, String locale) {
       String dirName;
       if (locale == null || locale.equals("en") || locale.equals("en-US")) {
           dirName = "values";  // 默认语言
       } else if (locale.contains("-")) {
           // zh-CN -> values-zh-rCN
           String[] parts = locale.split("-");
           dirName = "values-" + parts[0].toLowerCase() + "-r" + parts[1].toUpperCase();
       } else {
           dirName = "values-" + locale.toLowerCase();
       }
       return new File(outputDir, dirName);
   }
   ```

2. **XML 特殊字符转义**:

   ```java
   private String escapeXml(String value) {
       return value
               .replace("&", "&amp;")
               .replace("<", "&lt;")
               .replace(">", "&gt;")
               .replace("\"", "&quot;")
               .replace("'", "\\'");
   }
   ```

---

### 3.3 模型类详解

#### 3.3.1 ChartConfig.java

**文件路径**: `plugin/src/main/java/com/yourcompany/a11y/model/ChartConfig.java`

表示单个图表的配置信息。

```java
public class ChartConfig {
    private String id;       // 图表唯一标识
    private String type;     // 图表类型 (BAR, LINE, PIE, etc.)
    private String title;    // 图表标题
    private ChartData data;  // 图表数据
    private String context;  // 业务上下文描述

    // 将 ID 转换为合法的资源名称
    public String getResourceName() {
        return id.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}
```

#### 3.3.2 ChartType.java

**文件路径**: `plugin/src/main/java/com/yourcompany/a11y/model/ChartType.java`

支持的图表类型枚举，包含多语言名称。

```java
public enum ChartType {
    BAR("柱状图", "bar chart"),
    LINE("折线图", "line chart"),
    PIE("饼图", "pie chart"),
    SCATTER("散点图", "scatter chart"),
    RADAR("雷达图", "radar chart"),
    CANDLESTICK("K线图", "candlestick chart"),
    FUNNEL("漏斗图", "funnel chart"),
    GAUGE("仪表盘", "gauge chart"),
    HEATMAP("热力图", "heatmap"),
    TREEMAP("矩形树图", "treemap"),
    CUSTOM("自定义图表", "custom chart");

    public String getLocalizedName(String locale) {
        return locale.startsWith("zh") ? zhName : enName;
    }
}
```

#### 3.3.3 ChartData.java & Dataset.java

**文件路径**:
- `plugin/src/main/java/com/yourcompany/a11y/model/ChartData.java`
- `plugin/src/main/java/com/yourcompany/a11y/model/Dataset.java`

图表数据模型。

```java
public class ChartData {
    private List<String> labels;      // 数据标签 ["Q1", "Q2", "Q3", "Q4"]
    private List<Dataset> datasets;   // 数据集列表
}

public class Dataset {
    private String name;             // 数据集名称
    private List<Double> values;     // 数据值
    private String unit;             // 单位
    private String color;            // 颜色（可选）
}
```

#### 3.3.4 A11yResult.java & LocalizedDescription.java

**文件路径**:
- `plugin/src/main/java/com/yourcompany/a11y/model/A11yResult.java`
- `plugin/src/main/java/com/yourcompany/a11y/model/LocalizedDescription.java`

API 响应模型。

```java
public class A11yResult {
    // 语言 -> 描述 的映射
    private Map<String, LocalizedDescription> descriptions;

    public LocalizedDescription getForLocale(String locale) {
        return descriptions.get(locale);
    }
}

public class LocalizedDescription {
    private String brief;              // 简短描述
    private String detailed;           // 详细描述
    private List<String> dataPoints;   // 各数据点描述
}
```

---

## 4. Runtime 模块详解

### 4.1 模块概述

**路径**: `/runtime/`
**类型**: Android Library (AAR)
**包名**: `com.yourcompany.a11y.runtime`
**最低 SDK**: 21
**目标 SDK**: 34

Runtime 模块提供运行时 API，用于将编译时生成的无障碍描述应用到图表视图。

### 4.2 核心类详解

#### 4.2.1 ChartA11y.java

**文件路径**: `runtime/src/main/java/com/yourcompany/a11y/ChartA11y.java`

**实现原理**:

SDK 主入口，提供三种使用方式：

```java
// 方式 1: 简单 API
ChartA11y.apply(view, "chart_id");

// 方式 2: 指定描述类型
ChartA11y.apply(view, "chart_id", DescType.DETAILED);

// 方式 3: Builder 模式
ChartA11y.with(view)
    .chartId("chart_id")
    .descType(DescType.DETAILED)
    .focusable(true)
    .roleDescription("柱状图")
    .enableDataPointNavigation(true)
    .apply();
```

**Builder 类详解**:

```java
public static class Builder {
    private final View view;
    private String chartId;
    private DescType descType = DescType.BRIEF;
    private boolean focusable = true;
    private String roleDescription;
    private boolean enableDataPointNavigation = true;

    public void apply() {
        // 1. 获取资源名称
        String resourceName = toResourceName(chartId);

        // 2. 查找并设置 contentDescription
        String descResourceName = "a11y_chart_" + resourceName + "_" + descType.getSuffix();
        int descResId = resources.getIdentifier(descResourceName, "string", packageName);
        if (descResId != 0) {
            view.setContentDescription(resources.getString(descResId));
        }

        // 3. 设置焦点属性
        view.setFocusable(focusable);
        ViewCompat.setImportantForAccessibility(view, IMPORTANT_FOR_ACCESSIBILITY_YES);

        // 4. 创建并设置无障碍代理
        A11yDelegate delegate = new A11yDelegate(chartId, roleDescription);
        if (enableDataPointNavigation) {
            delegate.loadDataPoints(context, "a11y_chart_" + resourceName + "_point_");
        }
        ViewCompat.setAccessibilityDelegate(view, delegate);
    }
}
```

**关键技术**:

1. **动态资源查找**: 使用 `Resources.getIdentifier()` 在运行时查找字符串资源

   ```java
   int descResId = resources.getIdentifier(descResourceName, "string", packageName);
   ```

2. **ViewCompat 兼容性**: 使用 AndroidX 的 `ViewCompat` 确保向后兼容

   ```java
   ViewCompat.setImportantForAccessibility(view, IMPORTANT_FOR_ACCESSIBILITY_YES);
   ViewCompat.setAccessibilityDelegate(view, delegate);
   ```

---

#### 4.2.2 A11yDelegate.java

**文件路径**: `runtime/src/main/java/com/yourcompany/a11y/A11yDelegate.java`

**实现原理**:

继承 `AccessibilityDelegateCompat`，提供增强的无障碍功能。

**核心功能**:

| 功能 | 实现方法 |
|------|----------|
| **角色描述** | `onInitializeAccessibilityNodeInfo()` 设置 roleDescription |
| **数据点导航** | 自定义 AccessibilityAction 实现前/后导航 |
| **位置播报** | `onPopulateAccessibilityEvent()` 播报 "Item X of Y" |

**数据点导航机制**:

```
┌──────────────────────────────────────────────────────────────────┐
│                      数据点导航流程                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    TalkBack 用户操作                         │ │
│  │  ──────────────────────────────────────────────────────────  │ │
│  │  向右滑动 ──▶ "Next data point" Action                       │ │
│  │  向左滑动 ──▶ "Previous data point" Action                   │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                              │                                    │
│                              ▼                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │          performAccessibilityAction()                        │ │
│  │  ──────────────────────────────────────────────────────────  │ │
│  │  case ACTION_NEXT_DATA_POINT:                                │ │
│  │      currentDataPointIndex++;                                │ │
│  │      announceDataPoint(host);  // 播报当前数据点              │ │
│  │  case ACTION_PREVIOUS_DATA_POINT:                            │ │
│  │      currentDataPointIndex--;                                │ │
│  │      announceDataPoint(host);                                │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                              │                                    │
│                              ▼                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                 TalkBack 语音输出                            │ │
│  │  ──────────────────────────────────────────────────────────  │ │
│  │  "第二季度，销售额150万元。Item 2 of 4"                       │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

**关键代码**:

```java
// 自定义 Action ID
private static final int ACTION_NEXT_DATA_POINT = 0x10001;
private static final int ACTION_PREVIOUS_DATA_POINT = 0x10002;

@Override
public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
    super.onInitializeAccessibilityNodeInfo(host, info);

    // 设置角色描述
    if (roleDescription != null) {
        info.setRoleDescription(roleDescription);
    }

    // 添加自定义导航 Action
    if (!dataPointDescriptions.isEmpty()) {
        if (currentDataPointIndex < dataPointDescriptions.size() - 1) {
            info.addAction(new AccessibilityActionCompat(
                    ACTION_NEXT_DATA_POINT, "Next data point"));
        }
        if (currentDataPointIndex > 0) {
            info.addAction(new AccessibilityActionCompat(
                    ACTION_PREVIOUS_DATA_POINT, "Previous data point"));
        }
    }
}

@Override
public boolean performAccessibilityAction(View host, int action, Bundle args) {
    switch (action) {
        case ACTION_NEXT_DATA_POINT:
            currentDataPointIndex++;
            host.announceForAccessibility(getCurrentDataPointDescription() +
                    ". " + getStateDescription());
            return true;
        // ...
    }
    return super.performAccessibilityAction(host, action, args);
}
```

---

#### 4.2.3 DescType.java

**文件路径**: `runtime/src/main/java/com/yourcompany/a11y/DescType.java`

描述类型枚举。

```java
public enum DescType {
    BRIEF("brief"),      // 简短描述
    DETAILED("detailed"); // 详细描述

    private final String suffix;

    public String getSuffix() {
        return suffix;
    }
}
```

---

### 4.3 自定义属性

**文件路径**: `runtime/src/main/res/values/attrs.xml`

定义可在 XML 布局中使用的自定义属性。

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <declare-styleable name="ChartAccessibility">
        <!-- 图表唯一标识 -->
        <attr name="a11yChartId" format="string" />

        <!-- 是否可获取焦点 -->
        <attr name="a11yFocusable" format="boolean" />

        <!-- 描述类型 -->
        <attr name="a11yDescType" format="enum">
            <enum name="brief" value="0" />
            <enum name="detailed" value="1" />
        </attr>
    </declare-styleable>
</resources>
```

**使用示例**:

```xml
<com.github.mikephil.charting.charts.BarChart
    android:id="@+id/barChart"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    app:a11yChartId="sales_quarterly"
    app:a11yDescType="detailed"
    app:a11yFocusable="true" />
```

---

## 5. Sample 模块详解

### 5.1 模块概述

**路径**: `/sample/`
**类型**: Android Application
**包名**: `com.yourcompany.a11y.sample`

Sample 模块是一个示例应用，演示了插件和 SDK 的各种使用方式。

### 5.2 核心组件

#### 5.2.1 MainActivity.java

**文件路径**: `sample/src/main/java/com/yourcompany/a11y/sample/MainActivity.java`

展示四种不同的无障碍配置方式：

| 图表 | 配置方式 | 说明 |
|------|----------|------|
| **柱状图 (BarChart)** | XML 属性 | 使用 `app:a11yChartId`, `app:a11yDescType` |
| **折线图 (LineChart)** | 简单 API | `ChartA11y.apply(chart, "user_growth", DescType.DETAILED)` |
| **饼图 (PieChart)** | XML 属性 | 使用 `app:a11yChartId`, `app:a11yDescType` |
| **雷达图 (RadarChart)** | Builder 模式 | 完整的 Builder 链式调用 |

**代码示例**:

```java
// 方式 1: 柱状图 - XML 配置 (在 activity_main.xml 中)
// <BarChart app:a11yChartId="sales_quarterly" app:a11yDescType="detailed" />

// 方式 2: 折线图 - 简单 API
private void setupLineChart() {
    LineChart chart = findViewById(R.id.lineChart);
    // ... 设置图表数据 ...
    ChartA11y.apply(chart, "user_growth", DescType.DETAILED);
}

// 方式 3: 雷达图 - Builder 模式
private void setupRadarChart() {
    RadarChart chart = findViewById(R.id.radarChart);
    // ... 设置图表数据 ...
    ChartA11y.with(chart)
            .chartId("performance_radar")
            .descType(DescType.DETAILED)
            .focusable(true)
            .roleDescription("Radar Chart")
            .enableDataPointNavigation(true)
            .apply();
}
```

#### 5.2.2 chart_configs.json

**文件路径**: `sample/src/main/assets/chart_configs.json`

图表配置文件示例：

```json
{
  "version": "1.0",
  "charts": [
    {
      "id": "sales_quarterly",
      "type": "BAR",
      "title": "季度销售额",
      "data": {
        "labels": ["Q1", "Q2", "Q3", "Q4"],
        "datasets": [{
          "name": "销售额",
          "values": [120, 150, 180, 90],
          "unit": "万元"
        }]
      },
      "context": "用于展示公司年度销售趋势"
    },
    {
      "id": "user_growth",
      "type": "LINE",
      "title": "用户增长趋势",
      "data": {
        "labels": ["1月", "2月", "3月", "4月", "5月", "6月"],
        "datasets": [{
          "name": "新增用户",
          "values": [1200, 1500, 1800, 2200, 2800, 3500],
          "unit": "人"
        }]
      },
      "context": "展示上半年用户增长情况"
    }
  ]
}
```

#### 5.2.3 activity_main.xml

**文件路径**: `sample/src/main/res/layout/activity_main.xml`

布局文件演示 XML 属性配置：

```xml
<!-- 柱状图 - 使用 XML 属性配置无障碍 -->
<com.github.mikephil.charting.charts.BarChart
    android:id="@+id/barChart"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    app:a11yChartId="sales_quarterly"
    app:a11yDescType="detailed"
    app:a11yFocusable="true" />

<!-- 折线图 - 使用代码配置，无 XML 属性 -->
<com.github.mikephil.charting.charts.LineChart
    android:id="@+id/lineChart"
    android:layout_width="match_parent"
    android:layout_height="300dp" />
```

#### 5.2.4 build.gradle 配置

**文件路径**: `sample/build.gradle`

```groovy
plugins {
    id 'com.android.application'
    id 'com.yourcompany.chart-a11y'  // 应用插件
}

// 插件配置
chartA11y {
    configFile 'src/main/assets/chart_configs.json'
    apiEndpoint = 'https://api.example.com/a11y'
    apiKey = System.getenv('CHART_A11Y_API_KEY') ?: ''
    locales 'zh-CN', 'en'
    processLayouts = true
    enableCache = true
    cacheDir = file("${buildDir}/chart-a11y-cache")
    timeout = 30L
    concurrency = 4
    failOnError = false
}

dependencies {
    implementation project(':runtime')
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
}
```

---

## 6. 数据流程图

### 6.1 编译时数据流

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              编译时数据流                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐                                                        │
│  │ chart_configs.  │                                                        │
│  │ json            │                                                        │
│  └────────┬────────┘                                                        │
│           │                                                                  │
│           ▼                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                     GenerateDescriptionsTask                         │    │
│  ├─────────────────────────────────────────────────────────────────────┤    │
│  │                                                                      │    │
│  │  ChartConfigFile ──▶ List<ChartConfig>                              │    │
│  │                              │                                       │    │
│  │           ┌──────────────────┴──────────────────┐                   │    │
│  │           ▼                                     ▼                    │    │
│  │    ┌───────────┐                         ┌───────────┐              │    │
│  │    │CacheService│                         │A11yApi   │              │    │
│  │    │           │                         │Service    │              │    │
│  │    │  Cache    │◀─────── 缓存 ◀──────────│           │              │    │
│  │    │  Hit?     │                         │  fetch()  │──▶ AI API   │    │
│  │    └─────┬─────┘                         └─────┬─────┘              │    │
│  │          │                                     │                     │    │
│  │          └─────────────▶ A11yResult ◀──────────┘                    │    │
│  │                              │                                       │    │
│  │                              ▼                                       │    │
│  │                    ┌─────────────────┐                              │    │
│  │                    │StringResource   │                              │    │
│  │                    │Generator        │                              │    │
│  │                    └────────┬────────┘                              │    │
│  └─────────────────────────────┼────────────────────────────────────────┘   │
│                                │                                             │
│                                ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ build/generated/res/a11y/{variant}/                                  │    │
│  │ ├── values/chart_a11y_strings.xml                                   │    │
│  │ └── values-zh-rCN/chart_a11y_strings.xml                            │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────┐          ┌─────────────────────────────────────────┐   │
│  │ layout/*.xml    │    ───▶  │ ProcessLayoutsTask                      │   │
│  │ (with app:a11y  │          │ 注入 android:contentDescription 等      │   │
│  │  ChartId)       │          └─────────────────────────────────────────┘   │
│  └─────────────────┘                                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 运行时数据流

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              运行时数据流                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                        应用代码调用                                   │    │
│  │                                                                      │    │
│  │  ChartA11y.apply(view, "sales_quarterly", DescType.DETAILED)        │    │
│  │                                                                      │    │
│  │  或 Builder 模式:                                                    │    │
│  │  ChartA11y.with(view).chartId("xxx").descType(...).apply()          │    │
│  │                                                                      │    │
│  └────────────────────────────────┬─────────────────────────────────────┘   │
│                                   │                                          │
│                                   ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                        ChartA11y.Builder                             │    │
│  ├─────────────────────────────────────────────────────────────────────┤    │
│  │                                                                      │    │
│  │  1. 构建资源名称:                                                    │    │
│  │     "a11y_chart_" + resourceName + "_" + descType                   │    │
│  │     → "a11y_chart_sales_quarterly_detailed"                         │    │
│  │                                                                      │    │
│  │  2. 查找资源 ID:                                                     │    │
│  │     Resources.getIdentifier("a11y_chart_sales_quarterly_detailed",  │    │
│  │                             "string", packageName)                   │    │
│  │                                                                      │    │
│  │  3. 设置 View 属性:                                                  │    │
│  │     - setContentDescription(getString(resId))                       │    │
│  │     - setFocusable(true)                                            │    │
│  │     - setImportantForAccessibility(YES)                             │    │
│  │                                                                      │    │
│  │  4. 创建并设置无障碍代理:                                             │    │
│  │     A11yDelegate delegate = new A11yDelegate(chartId, roleDesc);    │    │
│  │     delegate.loadDataPoints(context, prefix);                        │    │
│  │     ViewCompat.setAccessibilityDelegate(view, delegate);            │    │
│  │                                                                      │    │
│  └────────────────────────────────┬─────────────────────────────────────┘   │
│                                   │                                          │
│                                   ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                        A11yDelegate                                  │    │
│  ├─────────────────────────────────────────────────────────────────────┤    │
│  │                                                                      │    │
│  │  ┌─────────────┐    ┌──────────────────────────────────────────────┐│   │
│  │  │ TalkBack    │───▶│ onInitializeAccessibilityNodeInfo()          ││   │
│  │  │ 获取节点信息 │    │ - 设置 roleDescription                       ││   │
│  │  └─────────────┘    │ - 添加 "Next/Previous data point" Action     ││   │
│  │                      └──────────────────────────────────────────────┘│   │
│  │                                                                      │    │
│  │  ┌─────────────┐    ┌──────────────────────────────────────────────┐│   │
│  │  │ 用户执行    │───▶│ performAccessibilityAction()                 ││   │
│  │  │ 自定义 Action│   │ - 更新 currentDataPointIndex                 ││   │
│  │  └─────────────┘    │ - announceForAccessibility() 播报            ││   │
│  │                      └──────────────────────────────────────────────┘│   │
│  │                                                                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. 技术栈总结

### 7.1 技术依赖表

| 模块 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **Plugin** | Gradle API | 8.x | 插件开发基础 |
| | Android Gradle Plugin | 8.1.0 | Android 构建集成 |
| | OkHttp | 4.11.0 | HTTP 客户端 |
| | Gson | 2.10.1 | JSON 处理 |
| | Java XML API | - | XML 解析/生成 |
| | JUnit | 4.13.2 | 单元测试 |
| | MockWebServer | 4.11.0 | API 测试 |
| **Runtime** | AndroidX Core | 1.12.0 | Android 兼容性 |
| | AndroidX Annotation | 1.7.0 | 注解支持 |
| | AccessibilityDelegateCompat | - | 无障碍代理 |
| **Sample** | MPAndroidChart | 3.1.0 | 图表库 |
| | AppCompat | 1.6.1 | UI 兼容性 |
| | Material Design | 1.11.0 | UI 组件 |

### 7.2 关键设计模式

| 模式 | 应用位置 | 说明 |
|------|----------|------|
| **Builder 模式** | `ChartA11y.Builder` | 灵活配置无障碍属性 |
| **工厂模式** | `ChartA11yPlugin.registerTasks()` | 创建 Gradle 任务 |
| **策略模式** | `DescType` 枚举 | 选择描述类型 |
| **代理模式** | `A11yDelegate` | 增强 View 无障碍功能 |
| **缓存模式** | `CacheService` | 避免重复 API 调用 |

### 7.3 安全考虑

| 方面 | 措施 |
|------|------|
| **API Key 保护** | 通过环境变量传入，不硬编码 |
| **缓存完整性** | 使用 SHA-256 哈希验证 |
| **XML 转义** | 正确处理特殊字符防止 XML 注入 |
| **ProGuard 规则** | 保护运行时反射调用的类 |

### 7.4 扩展性设计

1. **新增图表类型**: 在 `ChartType` 枚举中添加新类型
2. **新增语言**: 在 `chartA11y.locales` 配置中添加语言代码
3. **自定义 API**: 实现新的 API 服务类替换 `A11yApiService`
4. **自定义缓存策略**: 扩展 `CacheService` 添加过期机制

---

## 附录

### A. 资源命名规范

```
a11y_chart_{chartId}_{type}

chartId: 图表唯一标识（转换为小写下划线格式）
type: brief | detailed | point_{index}

示例:
- a11y_chart_sales_quarterly_brief
- a11y_chart_sales_quarterly_detailed
- a11y_chart_sales_quarterly_point_0
- a11y_chart_sales_quarterly_point_1
```

### B. 语言目录映射规则

```
语言代码          → Android 资源目录
────────────────────────────────────
en, en-US        → values/
zh-CN            → values-zh-rCN/
zh-TW            → values-zh-rTW/
ja               → values-ja/
ko               → values-ko/
fr-FR            → values-fr-rFR/
```

### C. 故障排查

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 资源未生成 | 配置文件路径错误 | 检查 `configFile` 路径 |
| API 调用失败 | 网络或认证问题 | 检查 `apiEndpoint` 和 `apiKey` |
| 无障碍不生效 | chartId 不匹配 | 确保代码和配置中 ID 一致 |
| 缓存无效 | 配置内容变更 | 清除 `cacheDir` 重新构建 |

---

*文档结束*
