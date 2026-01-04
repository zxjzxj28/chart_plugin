package com.yourcompany.a11y.service;

import com.yourcompany.a11y.model.A11yResult;
import com.yourcompany.a11y.model.ChartConfig;
import com.yourcompany.a11y.model.ChartPoint;
import com.yourcompany.a11y.model.LocalizedDescription;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DecimalFormat;

/**
 * Service for calling the remote A11y API to generate chart descriptions.
 * Supports concurrent requests, retry mechanism, and timeout configuration.
 */
public class A11yApiService {

    private static final MediaType TEXT = MediaType.get("text/plain; charset=utf-8");
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final String SUMMARY_PREFIX = "图表摘要";
    private static final String TEMPLATE_PREFIX = "数据点描述模版";
    private static final Pattern RESPONSE_PATTERN =
            Pattern.compile("图表摘要\\s*：\\s*([\\s\\S]*?)\\s*数据点描述模版\\s*：\\s*([\\s\\S]*)");

    private final String apiEndpoint;
    private final OkHttpClient client;
    private final ExecutorService executor;

    public A11yApiService(String apiEndpoint, long timeoutSeconds,
                          int concurrency) {
        this.apiEndpoint = apiEndpoint;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        this.executor = Executors.newFixedThreadPool(concurrency);
    }

    /**
     * Fetch accessibility descriptions for a single chart.
     *
     * @param config the chart configuration
     * @return the API result containing descriptions
     * @throws IOException if the API call fails after retries
     */
    public A11yResult fetchDescription(ChartConfig config) throws IOException {
        String requestBody = buildRequestBody(config);

        IOException lastException = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    // Exponential backoff
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                    Thread.sleep(backoffMs);
                }

                return executeRequest(requestBody, config);
            } catch (IOException e) {
                lastException = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        throw lastException != null ? lastException : new IOException("API call failed");
    }

    /**
     * Fetch accessibility descriptions for multiple charts concurrently.
     *
     * @param configs the list of chart configurations
     * @return map of chart ID to API result
     */
    public Map<String, A11yResult> fetchDescriptionsBatch(List<ChartConfig> configs) {
        Map<String, A11yResult> results = new HashMap<>();
        List<Future<Map.Entry<String, A11yResult>>> futures = new ArrayList<>();

        for (ChartConfig config : configs) {
            Callable<Map.Entry<String, A11yResult>> task = () -> {
                try {
                    A11yResult result = fetchDescription(config);
                    return Map.entry(config.getId(), result);
                } catch (IOException e) {
                    return Map.entry(config.getId(), (A11yResult) null);
                }
            };
            futures.add(executor.submit(task));
        }

        for (Future<Map.Entry<String, A11yResult>> future : futures) {
            try {
                Map.Entry<String, A11yResult> entry = future.get();
                if (entry.getValue() != null) {
                    results.put(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                // Skip failed requests
            }
        }

        return results;
    }

    private String buildRequestBody(ChartConfig config) {
        StringBuilder builder = new StringBuilder();
        builder.append("TITLE：").append(safeValue(config.getTitle())).append("\n");
        builder.append("TYPE：").append(safeValue(config.getType())).append("\n");

        boolean includeSeries = isMultiSeries(config.getData());
        builder.append(safeValue(config.getXLabel()))
                .append(" | ")
                .append(safeValue(config.getYLabel()));
        if (includeSeries) {
            builder.append(" | SERIES");
        }
        builder.append("\n");

        List<ChartPoint> points = config.getData();
        if (points != null) {
            for (ChartPoint point : points) {
                builder.append(safeValue(point.getXValue()))
                        .append(" | ")
                        .append(formatNumber(point.getYValue()));
                if (includeSeries) {
                    builder.append(" | ").append(safeValue(point.getSeries()));
                }
                builder.append("\n");
            }
        }

        Stats stats = computeStats(points);
        builder.append("MAX：").append(formatNumber(stats.max))
                .append(" MIN：").append(formatNumber(stats.min))
                .append(" AVG：").append(formatNumber(stats.avg))
                .append(" DIFF：").append(formatNumber(stats.diff));

        return builder.toString();
    }

    private A11yResult executeRequest(String requestBody, ChartConfig config) throws IOException {
        Request request = new Request.Builder()
                .url(apiEndpoint)
                .header("Content-Type", "text/plain")
                .post(RequestBody.create(requestBody, TEXT))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API request failed with status: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return parseResponse(responseBody, config);
        }
    }

    /**
     * Shutdown the executor service.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private A11yResult parseResponse(String responseBody, ChartConfig config) throws IOException {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IOException("Empty response body from API");
        }

        String summary;
        String template;
        Matcher matcher = RESPONSE_PATTERN.matcher(responseBody.trim());
        if (matcher.find()) {
            summary = matcher.group(1).trim();
            template = matcher.group(2).trim();
        } else {
            throw new IOException("Unexpected response format. Expected \"" + SUMMARY_PREFIX
                    + "：...\" and \"" + TEMPLATE_PREFIX + "：...\"");
        }

        List<String> dataPoints = buildDataPointDescriptions(template, config.getData());

        LocalizedDescription description = new LocalizedDescription();
        description.setBrief(summary);
        description.setDataPoints(dataPoints);

        A11yResult result = new A11yResult();
        result.addDescription("default", description);
        return result;
    }

    private List<String> buildDataPointDescriptions(String template, List<ChartPoint> points) {
        List<String> descriptions = new ArrayList<>();
        if (template == null || template.isBlank() || points == null) {
            return descriptions;
        }

        for (ChartPoint point : points) {
            String description = applyTemplate(template, point);
            if (description != null && !description.isBlank()) {
                descriptions.add(description.trim());
            }
        }

        return descriptions;
    }

    private String applyTemplate(String template, ChartPoint point) {
        String result = template;
        String xValue = safeValue(point.getXValue());
        String yValue = formatNumber(point.getYValue());
        String series = safeValue(point.getSeries());

        result = replaceToken(result, "{x}", xValue);
        result = replaceToken(result, "{x_value}", xValue);
        result = replaceToken(result, "{{x}}", xValue);
        result = replaceToken(result, "{y}", yValue);
        result = replaceToken(result, "{y_value}", yValue);
        result = replaceToken(result, "{{y}}", yValue);
        result = replaceToken(result, "{series}", series);
        result = replaceToken(result, "{{series}}", series);

        if (series.isEmpty()) {
            result = result.replaceAll("\\s*\\|?\\s*\\{\\{?series\\}?\\}\\s*", " ").trim();
        }

        return result;
    }

    private String replaceToken(String template, String token, String value) {
        return template.replace(token, value == null ? "" : value);
    }

    private boolean isMultiSeries(List<ChartPoint> points) {
        if (points == null) {
            return false;
        }
        Set<String> seriesNames = new TreeSet<>();
        for (ChartPoint point : points) {
            if (point != null && point.getSeries() != null && !point.getSeries().isBlank()) {
                seriesNames.add(point.getSeries().trim());
            }
        }
        return seriesNames.size() > 1;
    }

    private Stats computeStats(List<ChartPoint> points) {
        Stats stats = new Stats();
        if (points == null || points.isEmpty()) {
            return stats;
        }

        double sum = 0;
        int count = 0;
        for (ChartPoint point : points) {
            if (point == null || point.getYValue() == null) {
                continue;
            }
            double value = point.getYValue();
            stats.max = Math.max(stats.max, value);
            stats.min = Math.min(stats.min, value);
            sum += value;
            count++;
        }

        if (count > 0) {
            stats.avg = sum / count;
            stats.diff = stats.max - stats.min;
        } else {
            stats.max = 0;
            stats.min = 0;
            stats.avg = 0;
            stats.diff = 0;
        }
        return stats;
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "";
        }
        DecimalFormat format = new DecimalFormat("0.##");
        format.setDecimalSeparatorAlwaysShown(false);
        format.setGroupingUsed(false);
        return format.format(value);
    }

    private static class Stats {
        private double max = Double.NEGATIVE_INFINITY;
        private double min = Double.POSITIVE_INFINITY;
        private double avg = 0;
        private double diff = 0;
    }
}
