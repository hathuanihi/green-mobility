package com.greenmobility.common.controller;

import com.greenmobility.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> checkHealth() {
        return ApiResponse.ok("Green Mobility Core Backend is running", Map.of(
                "status", "UP",
                "service", "green-mobility-backend",
                "version", "1.0.0",
                "architecture", "Modular Monolith"
        ));
    }
}
