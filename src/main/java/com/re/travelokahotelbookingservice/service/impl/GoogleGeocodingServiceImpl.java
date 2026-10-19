package com.re.travelokahotelbookingservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.re.travelokahotelbookingservice.service.GoogleGeocodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class GoogleGeocodingServiceImpl implements GoogleGeocodingService {
    private final RestClient restClient;

    @Value("${google.maps.api-key:}")
    private String apiKey;

    public GoogleGeocodingServiceImpl(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String validateAddress(String address) {
        Map<String, Object> result = geocodeAddress(address);
        Object formatted = result.get("formattedAddress");
        return (formatted instanceof String str && !str.isBlank()) ? str : address;
    }

    @Override
    public Map<String, Object> geocodeAddress(String address) {
        Map<String, Object> result = new HashMap<>();
        result.put("originalAddress", address);

        if (address == null || address.isBlank()) {
            result.put("formattedAddress", address);
            result.put("status", "EMPTY_ADDRESS");
            return result;
        }

        // 1. Thử xác thực với Google Maps Geocoding API nếu đã cấu hình API Key
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                JsonNode response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("maps.googleapis.com")
                                .path("/maps/api/geocode/json")
                                .queryParam("address", address)
                                .queryParam("key", apiKey.trim())
                                .queryParam("language", "vi")
                                .queryParam("region", "vn")
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                if (response != null) {
                    String status = response.path("status").asText();
                    if ("OK".equals(status)) {
                        JsonNode results = response.path("results");
                        if (results.isArray() && !results.isEmpty()) {
                            JsonNode first = results.get(0);
                            String formatted = first.path("formatted_address").asText();
                            double lat = first.path("geometry").path("location").path("lat").asDouble(0.0);
                            double lng = first.path("geometry").path("location").path("lng").asDouble(0.0);

                            if (formatted != null && !formatted.isBlank()) {
                                log.info("Đã chuẩn hóa địa chỉ qua Google Maps Geocoding: [{}] -> [{}]", address, formatted);
                                result.put("formattedAddress", formatted);
                                result.put("latitude", lat);
                                result.put("longitude", lng);
                                result.put("provider", "GOOGLE_MAPS");
                                result.put("status", "OK");
                                return result;
                            }
                        }
                    } else {
                        log.warn("Google Maps Geocoding trả về trạng thái [{}]. Kích hoạt cơ chế OpenStreetMap Fallback...", status);
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể kết nối Google Maps Geocoding API ({}). Kích hoạt OpenStreetMap Fallback...", e.getMessage());
            }
        }

        // 2. CƠ CHẾ DỰ PHÒNG HOÀN HẢO: OpenStreetMap Nominatim Geocoding API (100% miễn phí, không cần API Key)
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("nominatim.openstreetmap.org")
                            .path("/search")
                            .queryParam("q", address)
                            .queryParam("countrycodes", "vn")
                            .queryParam("format", "json")
                            .queryParam("addressdetails", "1")
                            .queryParam("limit", "1")
                            .build())
                    .header("User-Agent", "TravelokaHotelBookingApp/1.0 (contact: huutran0991@gmail.com)")
                    .header("Accept-Language", "vi")
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.isArray() && !response.isEmpty()) {
                JsonNode first = response.get(0);
                String displayName = first.path("display_name").asText();
                double lat = first.path("lat").asDouble(0.0);
                double lng = first.path("lon").asDouble(0.0);

                if (displayName != null && !displayName.isBlank()) {
                    log.info("Đã chuẩn hóa địa chỉ qua OpenStreetMap Nominatim: [{}] -> [{}]", address, displayName);
                    result.put("formattedAddress", displayName);
                    result.put("latitude", lat);
                    result.put("longitude", lng);
                    result.put("provider", "OPEN_STREET_MAP");
                    result.put("status", "OK");
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("Nominatim Geocoding fallback notice: {}. Giữ nguyên địa chỉ gốc.", e.getMessage());
        }

        // 3. Nếu cả hai dịch vụ đều không có kết quả, giữ nguyên địa chỉ người dùng đã nhập
        result.put("formattedAddress", address);
        result.put("provider", "RAW_INPUT");
        result.put("status", "FALLBACK");
        return result;
    }
}
