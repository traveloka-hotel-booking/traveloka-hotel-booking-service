package com.re.travelokahotelbookingservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting DataInitializer: Checking and seeding sample hotels and rooms...");

        // Nạp Khách sạn và Phòng khớp với Frontend mockData
        initHotelsAndRooms();

        log.info("DataInitializer completed successfully.");
    }

    private void initHotelsAndRooms() {
        try {
            // Nạp Khách Sạn ID: 1 - Khách Sạn Grand Plaza Hà Nội
            String insertHotelSql = "INSERT INTO hotels (id, name, city, address, star_rating, description, thumbnail_url, amenities, created_at) " +
                    "VALUES (1, 'Khách Sạn Grand Plaza Hà Nội', 'Hà Nội', '117 Trần Duy Hưng, Cầu Giấy, Hà Nội', 5, " +
                    "'Khách sạn 5 sao Grand Plaza Hà Nội là biểu tượng xa hoa tọa lạc tại trung tâm khu đô thị mới phía Tây thủ đô.', " +
                    "'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&auto=format&fit=crop&q=60', " +
                    "'Wifi miễn phí, Hồ bơi trong nhà, Spa & Massage, Bữa sáng buffet, Phòng gym', NOW()) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
            jdbcTemplate.execute(insertHotelSql);

            // Nạp Phòng ID: 101 - Khớp Frontend
            String insertRoom101 = "INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status) " +
                    "VALUES (101, 1, 'Phòng Deluxe Giường Đôi Hướng Phố', 2, 1200000.00, 'AVAILABLE') " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
            jdbcTemplate.execute(insertRoom101);

            // Nạp Phòng ID: 102 - Khớp Frontend
            String insertRoom102 = "INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status) " +
                    "VALUES (102, 1, 'Phòng Executive Suite Sang Trọng', 3, 2400000.00, 'AVAILABLE') " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
            jdbcTemplate.execute(insertRoom102);

            // Nạp Phòng ID: 103 - Khớp Frontend (Family Suite Gia Đình)
            String insertRoom103 = "INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status) " +
                    "VALUES (103, 1, 'Phòng Family Suite Gia Đình', 4, 3200000.00, 'AVAILABLE') " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name), price_per_night = VALUES(price_per_night)";
            jdbcTemplate.execute(insertRoom103);

            log.info("Initialized sample Hotel ID 1 and Room IDs 101, 102, 103 successfully.");
        } catch (Exception e) {
            log.warn("Notice during hotel and room seed initialization: {}", e.getMessage());
        }
    }
}
