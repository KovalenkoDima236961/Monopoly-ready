package com.dimon.catanbackend.utils.api;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApiAnalyticsService {

    private final Map<String, ApiMetrics> metricsMap = new ConcurrentHashMap<>();

    /**
     * Records an API call by updating the metrics for the given endpoint.
     * If the endpoint does not yet have any metrics recorded, a new ApiMetrics instance is created.
     *
     * @param endpoint The name or signature of the API endpoint (e.g., "AdminController.getAllUser()").
     * @param duration The time taken to process the API request, in milliseconds.
     * @param success  A boolean indicating whether the API call was successful (true) or resulted in an error (false).
     */
    public void recordApiCall(String endpoint, long duration, boolean success) {
        metricsMap.computeIfAbsent(endpoint, k -> new ApiMetrics()).addCall(duration, success);
    }

    /**
     * Retrieves the metrics for a specific API endpoint.
     * If no metrics are found for the given endpoint, it returns an empty ApiMetrics instance.
     *
     * @param endpoint The name or signature of the API endpoint.
     * @return An ApiMetrics object containing the metrics for the specified endpoint.
     */
    public ApiMetrics getMetricsForEndpoint(String endpoint) {
        return metricsMap.getOrDefault(endpoint, new ApiMetrics());
    }

    /**
     * Retrieves the metrics for all API endpoints.
     * This method is useful for getting an overview of the performance and usage of all APIs.
     *
     * @return A map where the keys are endpoint names and the values are their corresponding metrics.
     */
    public Map<String, ApiMetrics> getAllMetrics() {
        return new HashMap<>(metricsMap);
    }
}
