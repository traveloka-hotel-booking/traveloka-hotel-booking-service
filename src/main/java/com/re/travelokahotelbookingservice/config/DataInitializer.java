package com.re.travelokahotelbookingservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final RedisConnectionFactory redisConnectionFactory;
    private final CacheManager cacheManager;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting DataInitializer: Cleaning demo accounts and sample hotel data for production readiness...");

        // 1. Dọn dẹp triệt để các tài khoản demo và dữ liệu khách sạn/phòng mẫu cũ trong DB
        cleanupDemoData();

        // 2. Khởi tạo/Cập nhật duy nhất tài khoản Quản trị viên (Admin) chính thức
        initRealAdminUser();

        // 3. Làm sạch bộ nhớ đệm Redis
        clearRedisCache();

        log.info("DataInitializer completed: System is now running strictly with real production data.");
    }

    private void cleanupDemoData() {
        try {
            // Tắt ràng buộc khóa ngoại tạm thời để xóa dọn dẹp sạch sẽ
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

            // 1. Xóa các thanh toán liên quan đến dữ liệu mẫu hoặc tài khoản demo
            jdbcTemplate.execute("DELETE FROM payments WHERE booking_id IN (" +
                    "SELECT b.id FROM bookings b WHERE b.room_id IN (" +
                    "SELECT r.id FROM rooms r WHERE r.hotel_id IN (1, 2, 3, 4, 5, 6)) OR b.user_id IN (" +
                    "SELECT u.id FROM users u WHERE u.email IN ('admin@traveloka.vn', 'customer@traveloka.vn') OR u.username IN ('admin', 'customer_demo')))");

            // 2. Xóa các đơn đặt phòng của khách sạn mẫu hoặc tài khoản demo
            jdbcTemplate.execute("DELETE FROM bookings WHERE room_id IN (" +
                    "SELECT r.id FROM rooms r WHERE r.hotel_id IN (1, 2, 3, 4, 5, 6)) OR user_id IN (" +
                    "SELECT u.id FROM users u WHERE u.email IN ('admin@traveloka.vn', 'customer@traveloka.vn') OR u.username IN ('admin', 'customer_demo'))");

            // 3. Xóa các phòng mẫu 101, 102, 103, 201, 202, 301, 302, 401, 501, 502, 601, 602 hoặc thuộc khách sạn mẫu 1-6
            jdbcTemplate.execute("DELETE FROM rooms WHERE hotel_id IN (1, 2, 3, 4, 5, 6) OR id IN (101, 102, 103, 201, 202, 301, 302, 401, 501, 502, 601, 602)");

            // 4. Xóa 6 khách sạn mẫu cũ (1-6)
            jdbcTemplate.execute("DELETE FROM hotels WHERE id IN (1, 2, 3, 4, 5, 6)");

            // 5. Xóa Refresh tokens của tài khoản demo
            jdbcTemplate.execute("DELETE FROM refresh_tokens WHERE user_id IN (" +
                    "SELECT u.id FROM users u WHERE u.email IN ('admin@traveloka.vn', 'customer@traveloka.vn') OR u.username IN ('admin', 'customer_demo'))");

            // 6. Xóa các tài khoản demo khỏi bảng users
            jdbcTemplate.execute("DELETE FROM users WHERE email IN ('admin@traveloka.vn', 'customer@traveloka.vn') OR username IN ('admin', 'customer_demo')");

            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            log.info("Successfully cleaned up all demo accounts, sample hotels (ID 1-6) and related test records.");
        } catch (Exception e) {
            log.warn("Notice during demo data cleanup: {}", e.getMessage());
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            } catch (Exception ignored) {}
        }
    }

    private void initRealAdminUser() {
        try {
            String adminEmail = "huutran0991@gmail.com";
            String adminPasswordHash = passwordEncoder.encode("Huu123456");

            // Đảm bảo cột thumbnail_url là TEXT để lưu trữ link ảnh dài không bị truncation
            try {
                jdbcTemplate.execute("ALTER TABLE hotels MODIFY COLUMN thumbnail_url TEXT");
            } catch (Exception ignored) {}

            // Chuẩn hóa Enum Gender
            try {
                jdbcTemplate.execute("UPDATE users SET gender = 'MALE' WHERE gender = 'Nam' OR gender = 'nam'");
                jdbcTemplate.execute("UPDATE users SET gender = 'FEMALE' WHERE gender = 'Nữ' OR gender = 'Nu' OR gender = 'nu'");
            } catch (Exception ignored) {}

            String upsertAdminSql = "INSERT INTO users (username, email, password, full_name, phone, role, is_enabled, created_at) " +
                    "VALUES ('huutran', ?, ?, 'Trần Văn Hữu', '0901234567', 'ROLE_ADMIN', true, NOW()) " +
                    "ON DUPLICATE KEY UPDATE password = VALUES(password), role = 'ROLE_ADMIN', is_enabled = true";
            jdbcTemplate.update(upsertAdminSql, adminEmail, adminPasswordHash);

            log.info("Initialized/Verified official Admin account ({}) successfully.", adminEmail);
        } catch (Exception e) {
            log.error("Failed to initialize official Admin account: {}", e.getMessage(), e);
        }
    }

    private void clearRedisCache() {
        try {
            if (cacheManager != null) {
                for (String cacheName : cacheManager.getCacheNames()) {
                    var cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                    }
                }
            }
            if (redisConnectionFactory != null) {
                try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                    connection.serverCommands().flushDb();
                }
            }
            log.info("Redis cache has been completely cleared.");
        } catch (Exception e) {
            log.warn("Notice while clearing Redis cache: {}", e.getMessage());
        }
    }
}
