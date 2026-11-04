package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.hotel.HotelFilterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemWarmupService {

    private final HotelService hotelService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${RENDER_EXTERNAL_URL:${APP_EXTERNAL_URL:}}")
    private String externalUrl;

    private final AtomicBoolean cacheWarmedUp = new AtomicBoolean(false);
    private final AtomicBoolean warmupInProgress = new AtomicBoolean(false);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public boolean isCacheWarmedUp() {
        return cacheWarmedUp.get();
    }

    /**
     * Làm nóng (Warm-up) bất đồng bộ các truy vấn trang chủ & trang danh sách khách sạn
     * ngay sau khi ứng dụng khởi động hoặc khi có yêu cầu đánh thức server.
     */
    @Async("auditLogExecutor")
    public void warmupInitialCachesAsync() {
        warmupInitialCaches();
    }

    public void warmupInitialCaches() {
        if (!warmupInProgress.compareAndSet(false, true)) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            HotelFilterRequest defaultFilter = new HotelFilterRequest();
            // 1. Làm nóng truy vấn mặc định của Trang chủ (HomePage: page=0, size=50)
            hotelService.filterHotels(defaultFilter, PageRequest.of(0, 50));

            // 2. Làm nóng truy vấn mặc định của Trang danh sách khách sạn (HotelsPage: page=0, size=10)
            hotelService.filterHotels(defaultFilter, PageRequest.of(0, 10));

            // 3. Làm nóng truy vấn tìm kiếm mặc định hôm nay -> ngày mai
            LocalDate today = LocalDate.now();
            hotelService.searchHotels(
                    "",
                    today,
                    today.plusDays(1),
                    2,
                    PageRequest.of(0, 50, Sort.by("name").ascending())
            );

            cacheWarmedUp.set(true);
            log.info("Cache warm-up completed in {} ms (HomePage & HotelsPage data ready in RAM/Redis).",
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Notice during initial cache warm-up: {}", e.getMessage());
        } finally {
            warmupInProgress.set(false);
        }
    }

    /**
     * Định kỳ mỗi 4 phút giữ kết nối MySQL (HikariCP) luôn ấm và làm mới trước bộ nhớ đệm
     * để người dùng đầu tiên truy cập không bao giờ gặp tình trạng chờ Cold Cache hoặc đứt kết nối DB.
     */
    @Scheduled(fixedDelay = 240_000, initialDelay = 180_000)
    public void keepConnectionsAndCacheWarm() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            warmupInitialCaches();
        } catch (Exception e) {
            log.debug("Background keep-alive check notice: {}", e.getMessage());
        }
    }

    /**
     * Tự động gửi tín hiệu Keep-Alive mỗi 10 phút tới URL public trên Render (nếu có RENDER_EXTERNAL_URL)
     * để hạn chế tối đa việc container bị ngủ đông (Cold Start) sau 15 phút không có truy cập.
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 300_000)
    public void pingExternalHealthEndpoint() {
        if (externalUrl == null || externalUrl.isBlank()) {
            return;
        }
        String targetUrl = externalUrl.replaceAll("/+$", "") + "/api/v1/health";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.debug("Self-ping skipped: {}", e.getMessage());
        }
    }
}
