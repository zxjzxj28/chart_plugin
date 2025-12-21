package com.yourcompany.a11y.service;

import com.yourcompany.a11y.model.A11yResult;
import com.yourcompany.a11y.model.ChartConfig;
import com.yourcompany.a11y.model.ChartData;
import com.yourcompany.a11y.model.Dataset;
import com.yourcompany.a11y.model.LocalizedDescription;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for CacheService.
 */
public class CacheServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File cacheDir;
    private CacheService cacheService;

    @Before
    public void setUp() throws Exception {
        cacheDir = tempFolder.newFolder("cache");
        cacheService = new CacheService(cacheDir, true);
    }

    @Test
    public void testComputeHash_sameConfigSameHash() {
        ChartConfig config1 = createTestConfig("test_chart");
        ChartConfig config2 = createTestConfig("test_chart");

        String hash1 = cacheService.computeHash(config1);
        String hash2 = cacheService.computeHash(config2);

        assertEquals(hash1, hash2);
    }

    @Test
    public void testComputeHash_differentConfigDifferentHash() {
        ChartConfig config1 = createTestConfig("chart_1");
        ChartConfig config2 = createTestConfig("chart_2");

        String hash1 = cacheService.computeHash(config1);
        String hash2 = cacheService.computeHash(config2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testPutAndGet_success() {
        ChartConfig config = createTestConfig("test_chart");
        A11yResult result = createTestResult();

        cacheService.put(config, result);
        A11yResult cached = cacheService.get(config);

        assertNotNull(cached);
        assertTrue(cached.hasDescriptions());
        assertEquals("Brief description", cached.getForLocale("en").getBrief());
    }

    @Test
    public void testGet_notCached() {
        ChartConfig config = createTestConfig("not_cached");
        A11yResult cached = cacheService.get(config);

        assertNull(cached);
    }

    @Test
    public void testIsCached_true() {
        ChartConfig config = createTestConfig("test_chart");
        A11yResult result = createTestResult();

        cacheService.put(config, result);

        assertTrue(cacheService.isCached(config));
    }

    @Test
    public void testIsCached_false() {
        ChartConfig config = createTestConfig("not_cached");

        assertFalse(cacheService.isCached(config));
    }

    @Test
    public void testCacheDisabled_putDoesNothing() {
        CacheService disabledCache = new CacheService(cacheDir, false);
        ChartConfig config = createTestConfig("test_chart");
        A11yResult result = createTestResult();

        disabledCache.put(config, result);
        A11yResult cached = disabledCache.get(config);

        assertNull(cached);
    }

    @Test
    public void testGetCacheSize() {
        ChartConfig config1 = createTestConfig("chart_1");
        ChartConfig config2 = createTestConfig("chart_2");
        A11yResult result = createTestResult();

        assertEquals(0, cacheService.getCacheSize());

        cacheService.put(config1, result);
        assertEquals(1, cacheService.getCacheSize());

        cacheService.put(config2, result);
        assertEquals(2, cacheService.getCacheSize());
    }

    @Test
    public void testClearAll() {
        ChartConfig config = createTestConfig("test_chart");
        A11yResult result = createTestResult();

        cacheService.put(config, result);
        assertEquals(1, cacheService.getCacheSize());

        cacheService.clearAll();
        assertEquals(0, cacheService.getCacheSize());
    }

    private ChartConfig createTestConfig(String id) {
        ChartConfig config = new ChartConfig();
        config.setId(id);
        config.setType("BAR");
        config.setTitle("Test Chart");

        ChartData data = new ChartData();
        data.setLabels(Arrays.asList("A", "B", "C"));

        Dataset dataset = new Dataset();
        dataset.setName("Test Data");
        dataset.setValues(Arrays.asList(10.0, 20.0, 30.0));
        dataset.setUnit("units");
        data.setDatasets(List.of(dataset));

        config.setData(data);
        return config;
    }

    private A11yResult createTestResult() {
        A11yResult result = new A11yResult();

        LocalizedDescription enDesc = new LocalizedDescription();
        enDesc.setBrief("Brief description");
        enDesc.setDetailed("Detailed description");
        enDesc.setDataPoints(Arrays.asList("Point 1", "Point 2", "Point 3"));

        result.addDescription("en", enDesc);
        return result;
    }
}
