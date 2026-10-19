package com.re.travelokahotelbookingservice.service;

import java.util.Map;

public interface GoogleGeocodingService {
    String validateAddress(String address);
    Map<String, Object> geocodeAddress(String address);
}
