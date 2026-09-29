package com.re.travelokahotelbookingservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {
    private static final Logger log = LoggerFactory.getLogger(UnauthorizedEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
            log.warn("Unauthorized access attempt. Path: {} | Message: {}",
                    request.getRequestURI(), authException.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("error", "Unauthorized");
            body.put("message", "Bạn chưa đăng nhập hoặc token không hợp lệ");
            body.put("status", 401);
            body.put("timestamp", LocalDateTime.now().toString());

            new ObjectMapper().writeValue(response.getOutputStream(), body);
    }
}
