package com.re.travelokahotelbookingservice.exception.handle;

import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.exception.BusinessException;
import com.re.travelokahotelbookingservice.exception.EmailAlreadyExistsException;
import com.re.travelokahotelbookingservice.exception.RoomNotAvailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RoomNotAvailableException.class)
    public ResponseEntity<Map<String, Object>> handleRoomNotAvailable(
            RoomNotAvailableException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", HttpStatus.CONFLICT.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntime(BusinessException ex) {

        String error = ex.getMessage();
        Map<String, Object> response = new HashMap<>();
        response.put("errors", error);
        ApiResponse<?> apiResponse = new ApiResponse<>(400, "BAD REQUEST", response);
        return ResponseEntity.badRequest().body(apiResponse);

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {

        String error = ex.getBindingResult().getFieldError().getDefaultMessage();
        Map<String, Object> response = new HashMap<>();
        response.put("errors", error);

        ApiResponse<?> apiResponse = new ApiResponse<>(400, "BAD REQUEST", response);

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<?>> handelEmailExists(EmailAlreadyExistsException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("errors", ex.getMessage());
        ApiResponse<?> apiResponse = new ApiResponse<>(400, "BAD REQUEST", response);
        return ResponseEntity.badRequest().body(apiResponse);
    }
}
