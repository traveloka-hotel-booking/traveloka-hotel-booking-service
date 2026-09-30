package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.dto.request.CreatePaymentRequest;
import com.re.travelokahotelbookingservice.dto.response.PaymentResponseDto;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentService {
    PaymentResponseDto createPayment(CreatePaymentRequest request, String currentUsername, HttpServletRequest httpRequest);
    PaymentResponseDto handleVNPayCallback(Map<String, String> allParams);
    PaymentResponseDto getPaymentByBookingId(Long bookingId, String currentUsername);
}
