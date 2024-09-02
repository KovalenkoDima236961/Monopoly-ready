package com.dimon.catanbackend.controller;

import com.dimon.catanbackend.utils.api.ApiAnalyticsService;
import com.dimon.catanbackend.utils.api.ApiMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private ApiAnalyticsService apiAnalyticsService;

    @GetMapping("/all")
    public Map<String, ApiMetrics> getAllMetrics() {
        return apiAnalyticsService.getAllMetrics();
    }

    @GetMapping("/endpoint")
    public ApiMetrics getMetricsForEndpoint(@RequestParam String endpoint) {
        return apiAnalyticsService.getMetricsForEndpoint(endpoint);
    }
}

