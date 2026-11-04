package com.re.travelokahotelbookingservice.controller;

import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.service.SystemWarmupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final SystemWarmupService systemWarmupService;

    /**
     * Endpoint kiểm tra trạng thái hoạt động (Health Check & Keep-Alive) siêu nhẹ,
     * phục vụ Render Health Check, UptimeRobot/Cron-job và tín hiệu Warm-up từ Frontend.
     */
    @GetMapping({"/api/v1/health", "/health"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck(
            @RequestParam(defaultValue = "false") boolean warmup
    ) {
        if (warmup || !systemWarmupService.isCacheWarmedUp()) {
            systemWarmupService.warmupInitialCachesAsync();
        }

        Map<String, Object> status = Map.of(
                "status", "UP",
                "cacheReady", systemWarmupService.isCacheWarmedUp(),
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new ApiResponse<>(200, "OK", status));
    }
}
