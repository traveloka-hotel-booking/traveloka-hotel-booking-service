package com.re.travelokahotelbookingservice.exception.handle;

import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.exception.BusinessException;
import com.re.travelokahotelbookingservice.exception.EmailAlreadyExistsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandle {

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
