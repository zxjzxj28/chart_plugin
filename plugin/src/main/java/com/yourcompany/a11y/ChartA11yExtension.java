package com.yourcompany.a11y;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.file.RegularFileProperty;

import java.io.File;

/**
 * DSL extension for configuring the Chart A11y plugin.
 *
 * Usage in build.gradle:
 * <pre>
 * chartA11y {
 *     configFile 'src/main/assets/chart_configs.json'
 *     apiEndpoint = 'https://api.example.com/a11y'
 *     apiKey = 'xxx'
 *     locales 'zh-CN', 'en'
 *     processLayouts = true
 *     enableCache = true
 *     cacheDir = file("${buildDir}/chart-a11y-cache")
 *     timeout = 30L
 *     concurrency = 4
 *     failOnError = false
 * }
 * </pre>
 */
public abstract class ChartA11yExtension {

    private final Project project;

    public ChartA11yExtension(Project project) {
        this.project = project;

        // Set default values
        getApiEndpoint().convention("https://api.example.com/a11y");
        getApiKey().convention("");
        getProcessLayouts().convention(true);
        getEnableCache().convention(true);
        getCacheDir().convention(new File(project.getBuildDir(), "chart-a11y-cache"));
        getTimeout().convention(30L);
        getConcurrency().convention(4);
        getFailOnError().convention(false);
    }

    /**
     * Path to the chart configuration JSON file.
     */
    public abstract Property<String> getConfigFile();

    /**
     * Remote API endpoint for generating accessibility descriptions.
     */
    public abstract Property<String> getApiEndpoint();

    /**
     * API key for authentication.
     */
    public abstract Property<String> getApiKey();

    /**
     * List of supported locales (e.g., "zh-CN", "en").
     */
    public abstract ListProperty<String> getLocales();

    /**
     * Whether to process layout XML files to inject accessibility attributes.
     */
    public abstract Property<Boolean> getProcessLayouts();

    /**
     * Whether to enable caching of API responses.
     */
    public abstract Property<Boolean> getEnableCache();

    /**
     * Directory for storing cached API responses.
     */
    public abstract Property<File> getCacheDir();

    /**
     * API request timeout in seconds.
     */
    public abstract Property<Long> getTimeout();

    /**
     * Number of concurrent API requests.
     */
    public abstract Property<Integer> getConcurrency();

    /**
     * Whether to fail the build on API errors.
     */
    public abstract Property<Boolean> getFailOnError();

    // Convenience methods for Groovy DSL

    /**
     * Set the config file path.
     */
    public void configFile(String path) {
        getConfigFile().set(path);
    }

    /**
     * Set the supported locales.
     */
    public void locales(String... locales) {
        getLocales().set(java.util.Arrays.asList(locales));
    }

    /**
     * Resolve the config file relative to the project.
     */
    public File resolveConfigFile() {
        String path = getConfigFile().getOrNull();
        if (path == null || path.isEmpty()) {
            return null;
        }
        return project.file(path);
    }
}
