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
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting DataInitializer: Checking and seeding sample users, hotels and rooms...");

        // 1. Nạp tài khoản Admin & Customer mẫu nếu chưa có
        initSampleUsers();

        // 2. Nạp Khách sạn và Phòng khớp với Frontend mockData
        initHotelsAndRooms();

        log.info("DataInitializer completed successfully.");
    }

    private void initSampleUsers() {
        try {
            String adminPasswordHash = passwordEncoder.encode("admin123");
            String insertAdminSql = "INSERT INTO users (username, email, password, full_name, phone, role, is_enabled, created_at) " +
                    "VALUES ('admin', 'admin@traveloka.vn', ?, 'Quản Trị Viên Traveloka', '0901234567', 'ROLE_ADMIN', true, NOW()) " +
                    "ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), role = 'ROLE_ADMIN', is_enabled = true";
            jdbcTemplate.update(insertAdminSql, adminPasswordHash);

            String customerPasswordHash = passwordEncoder.encode("123456");
            String insertCustomerSql = "INSERT INTO users (username, email, password, full_name, phone, role, is_enabled, created_at) " +
                    "VALUES ('customer_demo', 'customer@traveloka.vn', ?, 'Nguyễn Văn Khách', '0912345678', 'ROLE_CUSTOMER', true, NOW()) " +
                    "ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), is_enabled = true";
            jdbcTemplate.update(insertCustomerSql, customerPasswordHash);

            String huuPasswordHash = passwordEncoder.encode("Huu123456");
            String updateHuuSql = "UPDATE users SET password = ?, role = 'ROLE_ADMIN', is_enabled = true WHERE email = 'huutran0991@gmail.com'";
            jdbcTemplate.update(updateHuuSql, huuPasswordHash);

            try {
                jdbcTemplate.execute("UPDATE users SET gender = 'MALE' WHERE gender = 'Nam' OR gender = 'nam'");
                jdbcTemplate.execute("UPDATE users SET gender = 'FEMALE' WHERE gender = 'Nữ' OR gender = 'Nu' OR gender = 'nu'");
            } catch (Exception ignored) {}

            log.info("Initialized default admin and customer demo accounts successfully.");
        } catch (Exception e) {
            log.warn("Notice during user seed initialization: {}", e.getMessage());
        }
    }

    private void initHotelsAndRooms() {
        try {
            // Đảm bảo cột thumbnail_url là TEXT để lưu trữ link ảnh dài không bị lỗi truncation
            try {
                jdbcTemplate.execute("ALTER TABLE hotels MODIFY COLUMN thumbnail_url TEXT");
            } catch (Exception ignored) {}

            // Nạp Khách Sạn ID: 1 - Khách Sạn Grand Plaza Hà Nội
            String insertHotelSql = "INSERT INTO hotels (id, name, city, address, star_rating, description, thumbnail_url, amenities, created_at) " +
                    "VALUES (1, 'Khách Sạn Grand Plaza Hà Nội', 'Hà Nội', '117 Trần Duy Hưng, Cầu Giấy, Hà Nội', 5, " +
                    "'Khách sạn 5 sao Grand Plaza Hà Nội là biểu tượng xa hoa tọa lạc tại trung tâm khu đô thị mới phía Tây thủ đô.', " +
                    "'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&auto=format&fit=crop&q=60', " +
                    "'Wifi miễn phí, Hồ bơi trong nhà, Spa & Massage, Bữa sáng buffet, Phòng gym', NOW()) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
            jdbcTemplate.execute(insertHotelSql);

            // Nạp Phòng ID: 101 - Khớp Frontend
            String insertRoom101 = "INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (101, 1, 'Phòng Deluxe Giường Đôi Hướng Phố', 2, 1200000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&auto=format&fit=crop&q=60', " +
                    "'1 Giường đôi lớn (King Size)', '38 m²') " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))";
            jdbcTemplate.execute(insertRoom101);

            // Nạp Phòng ID: 102 - Khớp Frontend
            String insertRoom102 = "INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (102, 1, 'Phòng Executive Suite Sang Trọng', 3, 2400000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=600&auto=format&fit=crop&q=60', " +
                    "'1 Giường King & 1 Giường Sofa', '65 m²') " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))";
            jdbcTemplate.execute(insertRoom102);

            // Nạp Phòng ID: 103 - Khớp Frontend (Family Suite Gia Đình)
            String insertRoom103 = "INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (103, 1, 'Phòng Family Suite Gia Đình', 4, 3200000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=600&auto=format&fit=crop&q=60', " +
                    "'2 Giường King lớn', '75 m²') " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name), price_per_night = VALUES(price_per_night), image_url = COALESCE(rooms.image_url, VALUES(image_url))";
            jdbcTemplate.execute(insertRoom103);

            // Nạp Khách Sạn ID: 2 - Vinpearl Resort & Spa Đà Nẵng
            String insertHotel2 = "INSERT INTO hotels (id, name, city, address, star_rating, description, thumbnail_url, amenities, created_at) " +
                    "VALUES (2, 'Vinpearl Resort & Spa Đà Nẵng', 'Đà Nẵng', 'Đường Trường Sa, Ngũ Hành Sơn, Đà Nẵng', 5, " +
                    "'Tọa lạc tại vị trí đắc địa trên bãi biển Non Nước nguyên sơ.', " +
                    "'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800&auto=format&fit=crop&q=60', " +
                    "'Bãi biển riêng, Hồ bơi vô cực, Buffet quốc tế, Spa, Quầy bar', NOW()) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
            jdbcTemplate.execute(insertHotel2);
            jdbcTemplate.execute("INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (201, 2, 'Villa 2 Phòng Ngủ Hướng Biển Kèm Hồ Bơi Riêng', 4, 3500000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=600&auto=format&fit=crop&q=60', " +
                    "'2 Giường King lớn', '190 m²') ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))");
            jdbcTemplate.execute("INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (202, 2, 'Phòng Deluxe Hướng Đại Dương (Ocean View)', 2, 1800000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=600&auto=format&fit=crop&q=60', " +
                    "'1 Giường đôi Queen', '48 m²') ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))");

            // Nạp Khách Sạn ID: 3 - Rex Hotel Sài Gòn
            String insertHotel3 = "INSERT INTO hotels (id, name, city, address, star_rating, description, thumbnail_url, amenities, created_at) " +
                    "VALUES (3, 'Rex Hotel Sài Gòn', 'Hồ Chí Minh', '141 Nguyễn Huệ, Bến Nghé, Quận 1, TP. Hồ Chí Minh', 5, " +
                    "'Khách sạn kết hợp nét kiến trúc Pháp cổ điển ngay phố đi bộ Nguyễn Huệ.', " +
                    "'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&auto=format&fit=crop&q=60', " +
                    "'Vị trí Quận 1, Hồ bơi Rooftop, Rooftop Garden Bar, Buffet sáng', NOW()) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
            jdbcTemplate.execute(insertHotel3);
            jdbcTemplate.execute("INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (301, 3, 'Phòng Superior Cổ Điển Nhìn Ra Phố Đi Bộ', 2, 1100000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=600&auto=format&fit=crop&q=60', " +
                    "'1 Giường đôi hoặc 2 giường đơn', '36 m²') ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))");
            jdbcTemplate.execute("INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (302, 3, 'Phòng Premium Governor Suite', 2, 2100000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&auto=format&fit=crop&q=60', " +
                    "'1 Giường King siêu êm', '55 m²') ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))");

            // Nạp Khách Sạn ID: 4 - InterContinental Nha Trang
            String insertHotel4 = "INSERT INTO hotels (id, name, city, address, star_rating, description, thumbnail_url, amenities, created_at) " +
                    "VALUES (4, 'InterContinental Nha Trang', 'Nha Trang', '32-34 Trần Phú, Lộc Thọ, Nha Trang, Khánh Hòa', 5, " +
                    "'Khách sạn quốc tế 5 sao nằm ngay cung đường biển Trần Phú đẹp nhất vịnh Nha Trang.', " +
                    "'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=800&auto=format&fit=crop&q=60', " +
                    "'Mặt biển Trần Phú, 3 Hồ bơi ngoài trời, Spa cao cấp, Buffet sáng', NOW()) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
            jdbcTemplate.execute(insertHotel4);
            jdbcTemplate.execute("INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (401, 4, 'Phòng Classic Ocean View Hướng Biển', 2, 1950000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=600&auto=format&fit=crop&q=60', " +
                    "'1 Giường King hướng biển', '45 m²') ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))");

            // Nạp Khách Sạn ID: 5 - Dalat Edensee Lake Resort & Spa
            String insertHotel5 = "INSERT INTO hotels (id, name, city, address, star_rating, description, thumbnail_url, amenities, created_at) " +
                    "VALUES (5, 'Dalat Edensee Lake Resort & Spa', 'Đà Lạt', 'Khu chức năng VII.2, Hồ Tuyền Lâm, Đà Lạt, Lâm Đồng', 5, " +
                    "'Ngôi làng châu Âu thơ mộng nép mình bên bờ hồ Tuyền Lâm.', " +
                    "'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&auto=format&fit=crop&q=60', " +
                    "'View hồ Tuyền Lâm, Spa thảo dược, Xe điện đưa đón, Bắn cung', NOW()) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
            jdbcTemplate.execute(insertHotel5);
            jdbcTemplate.execute("INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (501, 5, 'Phòng Mimosa Superior Nhìn Ra Đồi Thông', 2, 1450000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=600&auto=format&fit=crop&q=60', " +
                    "'1 Giường đôi Queen', '40 m²') ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))");
            jdbcTemplate.execute("INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (502, 5, 'Biệt Thự Jasmine Luxury Lake View', 3, 2800000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&auto=format&fit=crop&q=60', " +
                    "'1 Giường King & 1 Giường Sofa', '80 m²') ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))");

            // Nạp Khách Sạn ID: 6 - JW Marriott Phu Quoc Emerald Bay Resort & Spa
            String insertHotel6 = "INSERT INTO hotels (id, name, city, address, star_rating, description, thumbnail_url, amenities, created_at) " +
                    "VALUES (6, 'JW Marriott Phu Quoc Emerald Bay Resort & Spa', 'Đảo Phú Quốc', 'Bãi Khem, An Thới, TP. Phú Quốc, Kiên Giang', 5, " +
                    "'Kiệt tác kiến trúc của kiến trúc sư Bill Bensley bên bờ biển Bãi Khem.', " +
                    "'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&auto=format&fit=crop&q=60', " +
                    "'Bãi biển Bãi Khem, Hồ bơi vỏ sò, Chanterelle Spa, Nhà hàng Pink Pearl', NOW()) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)";
            jdbcTemplate.execute(insertHotel6);
            jdbcTemplate.execute("INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (601, 6, 'Phòng Emerald Bay View Ban Công Riêng', 2, 4200000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&auto=format&fit=crop&q=60', " +
                    "'1 Giường đôi King', '53 m²') ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))");
            jdbcTemplate.execute("INSERT INTO rooms (id, hotel_id, name, max_guests, price_per_night, status, image_url, bed_type, room_size) " +
                    "VALUES (602, 6, 'Turquoise Suite 1 Phòng Ngủ Thượng Hạng', 3, 6800000.00, 'AVAILABLE', " +
                    "'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=600&auto=format&fit=crop&q=60', " +
                    "'1 Giường King siêu rộng', '90 m²') ON DUPLICATE KEY UPDATE name = VALUES(name), image_url = COALESCE(rooms.image_url, VALUES(image_url))");

            // Đảm bảo tất cả các phòng hiện có trong database nếu thiếu ảnh đều có ảnh mặc định chất lượng cao
            jdbcTemplate.execute("UPDATE rooms SET image_url = 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600&auto=format&fit=crop&q=60' WHERE image_url IS NULL OR image_url = ''");

            log.info("Initialized sample Hotels (ID 1-6) and all corresponding rooms successfully.");
        } catch (Exception e) {
            log.warn("Notice during hotel and room seed initialization: {}", e.getMessage());
        }
    }
}
