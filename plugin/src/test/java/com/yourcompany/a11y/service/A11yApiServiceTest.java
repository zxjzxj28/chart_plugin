package com.yourcompany.a11y.service;

import com.yourcompany.a11y.model.A11yResult;
import com.yourcompany.a11y.model.ChartConfig;
import com.yourcompany.a11y.model.ChartData;
import com.yourcompany.a11y.model.Dataset;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for A11yApiService using MockWebServer.
 */
public class A11yApiServiceTest {

    private MockWebServer mockServer;
    private A11yApiService apiService;

    @Before
    public void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        String endpoint = mockServer.url("/api/v1/chart-a11y").toString();
        apiService = new A11yApiService(
                endpoint,
                "test-api-key",
                10L,
                2,
                Arrays.asList("zh-CN", "en")
        );
    }

    @After
    public void tearDown() throws IOException {
        apiService.shutdown();
        mockServer.shutdown();
    }

    @Test
    public void testFetchDescription_success() throws IOException {
        // Prepare mock response
        String responseBody = """
            {
              "descriptions": {
                "zh-CN": {
                  "brief": "柱状图显示四个季度销售额",
                  "detailed": "这是一个柱状图，展示四个季度销售额",
                  "dataPoints": ["第一季度，120万元", "第二季度，150万元"]
                },
                "en": {
                  "brief": "Bar chart showing quarterly sales",
                  "detailed": "This is a bar chart showing quarterly sales",
                  "dataPoints": ["Q1, 1.2M", "Q2, 1.5M"]
                }
              }
            }
            """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        ChartConfig config = createTestConfig();
        A11yResult result = apiService.fetchDescription(config);

        assertNotNull(result);
        assertTrue(result.hasDescriptions());

        // Verify Chinese descriptions
        assertNotNull(result.getForLocale("zh-CN"));
        assertEquals("柱状图显示四个季度销售额", result.getForLocale("zh-CN").getBrief());

        // Verify English descriptions
        assertNotNull(result.getForLocale("en"));
        assertEquals("Bar chart showing quarterly sales", result.getForLocale("en").getBrief());
    }

    @Test(expected = IOException.class)
    public void testFetchDescription_serverError() throws IOException {
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        ChartConfig config = createTestConfig();
        apiService.fetchDescription(config);
    }

    @Test
    public void testFetchDescription_retryOnFailure() throws IOException {
        // First two attempts fail, third succeeds
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"descriptions\":{}}")
                .addHeader("Content-Type", "application/json"));

        ChartConfig config = createTestConfig();
        A11yResult result = apiService.fetchDescription(config);

        assertNotNull(result);
        assertEquals(3, mockServer.getRequestCount());
    }

    private ChartConfig createTestConfig() {
        ChartConfig config = new ChartConfig();
        config.setId("sales_quarterly");
        config.setType("BAR");
        config.setTitle("季度销售额");

        ChartData data = new ChartData();
        data.setLabels(Arrays.asList("Q1", "Q2", "Q3", "Q4"));

        Dataset dataset = new Dataset();
        dataset.setName("销售额");
        dataset.setValues(Arrays.asList(120.0, 150.0, 180.0, 90.0));
        dataset.setUnit("万元");
        data.setDatasets(List.of(dataset));

        config.setData(data);
        return config;
    }
}
