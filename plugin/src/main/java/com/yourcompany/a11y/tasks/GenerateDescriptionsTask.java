package com.yourcompany.a11y.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yourcompany.a11y.generator.StringResourceGenerator;
import com.yourcompany.a11y.model.A11yResult;
import com.yourcompany.a11y.model.ChartConfig;
import com.yourcompany.a11y.model.ChartConfigFile;
import com.yourcompany.a11y.service.A11yApiService;
import com.yourcompany.a11y.service.CacheService;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gradle task for generating accessibility description resources.
 *
 * This task:
 * 1. Reads and parses chart_configs.json
 * 2. Computes hash for each chart configuration
 * 3. Checks local cache for existing results
 * 4. Calls remote API for uncached charts
 * 5. Saves results to cache
 * 6. Generates string resource files for each locale
 */
public abstract class GenerateDescriptionsTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getConfigFile();

    @Input
    public abstract Property<String> getApiEndpoint();

    @Input
    @Optional
    public abstract Property<String> getApiKey();

    @Input
    public abstract ListProperty<String> getLocales();

    @Input
    public abstract Property<Boolean> getEnableCache();

    @Input
    public abstract Property<File> getCacheDir();

    @Input
    public abstract Property<Long> getTimeout();

    @Input
    public abstract Property<Integer> getConcurrency();

    @Input
    public abstract Property<Boolean> getFailOnError();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    private final Gson gson = new GsonBuilder().create();

    @TaskAction
    public void generate() {
        File configFile = getConfigFile().get().getAsFile();
        if (!configFile.exists()) {
            getLogger().warn("Chart config file not found: " + configFile.getAbsolutePath());
            return;
        }

        ChartConfigFile chartConfigFile = parseConfigFile(configFile);
        if (chartConfigFile == null || chartConfigFile.getCharts() == null ||
                chartConfigFile.getCharts().isEmpty()) {
            getLogger().warn("No charts found in config file");
            return;
        }

        List<ChartConfig> charts = chartConfigFile.getCharts();
        getLogger().lifecycle("Found {} chart configurations", charts.size());

        // Initialize services
        CacheService cacheService = new CacheService(
                getCacheDir().get(),
                getEnableCache().get()
        );

        List<String> locales = getLocales().get();
        A11yApiService apiService = new A11yApiService(
                getApiEndpoint().get(),
                getApiKey().getOrElse(""),
                getTimeout().get(),
                getConcurrency().get(),
                locales
        );

        // Process charts
        Map<String, A11yResult> results = new HashMap<>();
        List<ChartConfig> uncachedCharts = new ArrayList<>();

        // Check cache first
        for (ChartConfig chart : charts) {
            A11yResult cached = cacheService.get(chart);
            if (cached != null) {
                getLogger().info("Cache hit for chart: {}", chart.getId());
                results.put(chart.getId(), cached);
            } else {
                uncachedCharts.add(chart);
            }
        }

        // Fetch uncached charts from API
        if (!uncachedCharts.isEmpty()) {
            getLogger().lifecycle("Fetching {} charts from API", uncachedCharts.size());

            try {
                Map<String, A11yResult> apiResults = apiService.fetchDescriptionsBatch(uncachedCharts);

                for (ChartConfig chart : uncachedCharts) {
                    A11yResult result = apiResults.get(chart.getId());
                    if (result != null) {
                        results.put(chart.getId(), result);
                        cacheService.put(chart, result);
                        getLogger().info("Fetched and cached: {}", chart.getId());
                    } else {
                        getLogger().warn("Failed to fetch description for chart: {}", chart.getId());
                        if (getFailOnError().get()) {
                            throw new GradleException("Failed to fetch description for chart: " + chart.getId());
                        }
                    }
                }
            } finally {
                apiService.shutdown();
            }
        }

        // Generate resource files
        if (!results.isEmpty()) {
            generateResources(charts, results, locales);
        }
    }

    private ChartConfigFile parseConfigFile(File configFile) {
        try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, ChartConfigFile.class);
        } catch (IOException e) {
            getLogger().error("Failed to parse config file: {}", e.getMessage());
            if (getFailOnError().get()) {
                throw new GradleException("Failed to parse config file", e);
            }
            return null;
        }
    }

    private void generateResources(List<ChartConfig> charts, Map<String, A11yResult> results,
                                   List<String> locales) {
        StringResourceGenerator generator = new StringResourceGenerator();
        File outputDir = getOutputDir().get().getAsFile();

        try {
            generator.generate(outputDir, charts, results, locales);
            getLogger().lifecycle("Generated string resources in: {}", outputDir.getAbsolutePath());
        } catch (IOException e) {
            getLogger().error("Failed to generate resources: {}", e.getMessage());
            if (getFailOnError().get()) {
                throw new GradleException("Failed to generate resources", e);
            }
        }
    }
}
