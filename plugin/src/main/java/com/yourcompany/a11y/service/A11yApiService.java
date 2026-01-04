package com.yourcompany.a11y.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.yourcompany.a11y.model.A11yResult;
import com.yourcompany.a11y.model.ChartConfig;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Service for calling the remote A11y API to generate chart descriptions.
 * Supports concurrent requests, retry mechanism, and timeout configuration.
 */
public class A11yApiService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final String apiEndpoint;
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Gson gson;
    private final File imageBasePath;

    public A11yApiService(String apiEndpoint, long timeoutSeconds,
                          int concurrency, File imageBasePath) {
        this.apiEndpoint = apiEndpoint;
        this.imageBasePath = imageBasePath;
        this.gson = new GsonBuilder().create();

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

                return executeRequest(requestBody);
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
        JsonObject root = new JsonObject();

        // Chart object
        JsonObject chart = new JsonObject();
        chart.addProperty("id", config.getId());
        chart.addProperty("type", config.getType());
        chart.addProperty("title", config.getTitle());
        chart.add("data", gson.toJsonTree(config.getData()));

        // Add image data if available
        if (config.hasImage()) {
            String imageBase64 = readImageAsBase64(config.getImagePath());
            if (imageBase64 != null) {
                chart.addProperty("image", imageBase64);
                chart.addProperty("imageMimeType", getMimeType(config.getImagePath()));
            }
        }

        root.add("chart", chart);

        // Options
        JsonObject options = new JsonObject();
        options.addProperty("includeBrief", true);
        options.addProperty("includeDetailed", false);
        options.addProperty("includeDataPoints", true);
        root.add("options", options);

        return gson.toJson(root);
    }

    /**
     * Read an image file and encode it as Base64.
     *
     * @param imagePath the path to the image file (relative to imageBasePath or absolute)
     * @return Base64 encoded string, or null if the file cannot be read
     */
    private String readImageAsBase64(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }

        File imageFile = resolveImageFile(imagePath);
        if (imageFile == null || !imageFile.exists() || !imageFile.isFile()) {
            return null;
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Resolve the image file path.
     * If the path is absolute, use it directly.
     * If the path is relative, resolve it against the imageBasePath.
     *
     * @param imagePath the image path from config
     * @return the resolved File object
     */
    private File resolveImageFile(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }

        File file = new File(imagePath);
        if (file.isAbsolute()) {
            return file;
        }

        if (imageBasePath != null) {
            return new File(imageBasePath, imagePath);
        }

        return file;
    }

    /**
     * Get the MIME type based on file extension.
     *
     * @param imagePath the image file path
     * @return the MIME type string
     */
    private String getMimeType(String imagePath) {
        if (imagePath == null) {
            return "application/octet-stream";
        }

        String lowerPath = imagePath.toLowerCase();
        if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerPath.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerPath.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerPath.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private A11yResult executeRequest(String requestBody) throws IOException {
        Request request = new Request.Builder()
                .url(apiEndpoint)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API request failed with status: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return gson.fromJson(responseBody, A11yResult.class);
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
}
