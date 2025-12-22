package com.yourcompany.a11y.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yourcompany.a11y.model.A11yResult;
import com.yourcompany.a11y.model.ChartConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service for caching API responses to avoid redundant calls.
 * Uses SHA-256 hash of chart configuration as cache key.
 */
public class CacheService {

    private final File cacheDir;
    private final Gson gson;
    private final boolean enabled;
    private final File imageBasePath;

    public CacheService(File cacheDir, boolean enabled) {
        this(cacheDir, enabled, null);
    }

    public CacheService(File cacheDir, boolean enabled, File imageBasePath) {
        this.cacheDir = cacheDir;
        this.enabled = enabled;
        this.imageBasePath = imageBasePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        if (enabled && !cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    /**
     * Compute SHA-256 hash for a chart configuration.
     * If the chart has an associated image, the image content is also included in the hash.
     *
     * @param config the chart configuration
     * @return the hex-encoded hash string
     */
    public String computeHash(ChartConfig config) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Hash the JSON config
            String json = gson.toJson(config);
            digest.update(json.getBytes(StandardCharsets.UTF_8));

            // If the chart has an image, also hash the image content
            if (config.hasImage()) {
                byte[] imageBytes = readImageBytes(config.getImagePath());
                if (imageBytes != null) {
                    digest.update(imageBytes);
                }
            }

            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Read image file as byte array.
     *
     * @param imagePath the path to the image file
     * @return byte array of image content, or null if file cannot be read
     */
    private byte[] readImageBytes(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }

        File imageFile = resolveImageFile(imagePath);
        if (imageFile == null || !imageFile.exists() || !imageFile.isFile()) {
            return null;
        }

        try {
            return Files.readAllBytes(imageFile.toPath());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Resolve the image file path.
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

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Get cached result for a chart configuration.
     *
     * @param config the chart configuration
     * @return the cached result, or null if not found or expired
     */
    public A11yResult get(ChartConfig config) {
        if (!enabled) {
            return null;
        }

        String hash = computeHash(config);
        File cacheFile = getCacheFile(hash);

        if (!cacheFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(cacheFile, StandardCharsets.UTF_8)) {
            CacheEntry entry = gson.fromJson(reader, CacheEntry.class);
            if (entry != null && entry.result != null) {
                return entry.result;
            }
        } catch (IOException e) {
            // Cache read failed, return null
        }

        return null;
    }

    /**
     * Store a result in the cache.
     *
     * @param config the chart configuration
     * @param result the API result to cache
     */
    public void put(ChartConfig config, A11yResult result) {
        if (!enabled || result == null) {
            return;
        }

        String hash = computeHash(config);
        File cacheFile = getCacheFile(hash);

        CacheEntry entry = new CacheEntry();
        entry.dataHash = hash;
        entry.result = result;
        entry.cachedAt = System.currentTimeMillis();

        try (FileWriter writer = new FileWriter(cacheFile, StandardCharsets.UTF_8)) {
            gson.toJson(entry, writer);
        } catch (IOException e) {
            // Cache write failed, ignore
        }
    }

    /**
     * Check if a result is cached for a chart configuration.
     *
     * @param config the chart configuration
     * @return true if cached
     */
    public boolean isCached(ChartConfig config) {
        if (!enabled) {
            return false;
        }
        String hash = computeHash(config);
        return getCacheFile(hash).exists();
    }

    /**
     * Clear all cached entries.
     */
    public void clearAll() {
        if (!enabled || !cacheDir.exists()) {
            return;
        }

        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    /**
     * Get cache statistics.
     *
     * @return number of cached entries
     */
    public int getCacheSize() {
        if (!enabled || !cacheDir.exists()) {
            return 0;
        }

        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".json"));
        return files != null ? files.length : 0;
    }

    private File getCacheFile(String hash) {
        return new File(cacheDir, hash + ".json");
    }

    /**
     * Internal cache entry structure.
     */
    private static class CacheEntry {
        String dataHash;
        A11yResult result;
        long cachedAt;
    }
}
