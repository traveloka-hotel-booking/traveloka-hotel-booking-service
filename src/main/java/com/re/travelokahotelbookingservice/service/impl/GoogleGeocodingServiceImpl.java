package com.re.travelokahotelbookingservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.re.travelokahotelbookingservice.service.GoogleGeocodingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GoogleGeocodingServiceImpl implements GoogleGeocodingService {
    private final RestClient restClient;

    @Value("${google.maps.api-key}")
    private String apiKey;

    public GoogleGeocodingServiceImpl(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String validateAddress(String address) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/geocode/json")
                        .queryParam("address", address)
                        .queryParam("key", apiKey)
                        .queryParam("language", "vi")
                        .queryParam("region", "vn")
                        .build())
                .retrieve()
                .body(JsonNode.class);
        String status = response.path("status").asText();

        if ("REQUEST_DENIED".equals(status)) {
            //Google Geocoding API chưa được cấp quyền hoặc Billing chưa được kích hoạt.
            // Tạm thời cho phép lưu địa chỉ người dùng nhập
            return address;
        }

        if(!"OK".equals(status)) {
            throw new IllegalArgumentException("Địa chỉ không hợp lệ hoặc không tìm thấy");
        }

        JsonNode results = response.path("results");
        if(!results.isArray() || results.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy địa chỉ");
        }

        return results.get(0).path("formatted_address").asText();
    }
}
