package com.re.travelokahotelbookingservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.re.travelokahotelbookingservice.service.GoogleGeocodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
        if (address == null || address.isBlank()) {
            return address;
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("Google Maps API Key chưa được cấu hình. Giữ nguyên địa chỉ gốc: [{}]", address);
            return address;
        }

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

            if (response == null) {
                return address;
            }

            String status = response.path("status").asText();

            if ("OK".equals(status)) {
                JsonNode results = response.path("results");
                if (results.isArray() && !results.isEmpty()) {
                    String formattedAddress = results.get(0).path("formatted_address").asText();
                    if (formattedAddress != null && !formattedAddress.isBlank()) {
                        log.info("Đã chuẩn hóa địa chỉ qua Google Geocoding: [{}] -> [{}]", address, formattedAddress);
                        return formattedAddress;
                    }
                }
            } else {
                log.warn("Google Geocoding trả về trạng thái [{}]. Giữ nguyên địa chỉ gốc: [{}]", status, address);
            }
        } catch (Exception e) {
            log.warn("Không thể kết nối hoặc xác thực với Google Geocoding API ({}). Giữ nguyên địa chỉ: [{}]", e.getMessage(), address);
        }

        return address;
    }
}
