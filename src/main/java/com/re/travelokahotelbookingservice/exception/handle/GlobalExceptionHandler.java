package com.re.travelokahotelbookingservice.exception.handle;

import com.re.travelokahotelbookingservice.dto.response.ApiResponse;
import com.re.travelokahotelbookingservice.exception.BusinessException;
import com.re.travelokahotelbookingservice.exception.EmailAlreadyExistsException;
import com.re.travelokahotelbookingservice.exception.RoomNotAvailableException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RoomNotAvailableException.class)
    public ResponseEntity<ApiResponse<?>> handleRoomNotAvailable(RoomNotAvailableException ex) {
        log.warn("Room not available: {}", ex.getMessage());
        String msg = ex.getMessage();
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.CONFLICT.value(), "CONFLICT", msg, response);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResponse);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        String msg = ex.getMessage();
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "NOT FOUND", msg, response);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        String msg = ex.getMessage();
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "BAD REQUEST", msg, response);
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        String msg = ex.getMessage();
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "BAD REQUEST", msg, response);
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        String msg = "Không thể xóa hoặc thay đổi dữ liệu do đang có ràng buộc liên quan (đơn đặt phòng, thông tin lưu trú, v.v.)!";
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.CONFLICT.value(), "CONFLICT", msg, response);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        String error = Optional.ofNullable(ex.getBindingResult().getFieldError())
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Dữ liệu đầu vào không hợp lệ");
        log.warn("Validation error: {}", error);

        Map<String, Object> response = new HashMap<>();
        response.put("errors", error);
        response.put("message", error);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "BAD REQUEST", error, response);
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<?>> handleEmailExists(EmailAlreadyExistsException ex) {
        log.warn("Email already exists: {}", ex.getMessage());
        String msg = ex.getMessage();
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "BAD REQUEST", msg, response);
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt: {}", ex.getMessage());
        String msg = "Email hoặc mật khẩu không chính xác!";
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "BAD REQUEST", msg, response);
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleUsernameNotFound(UsernameNotFoundException ex) {
        log.warn("User not found attempt: {}", ex.getMessage());
        String msg = "Email hoặc mật khẩu không chính xác!";
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "BAD REQUEST", msg, response);
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        String msg = "Bạn không có quyền thực hiện thao tác này!";
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", msg, response);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        String msg = "Xác thực không thành công. Vui lòng đăng nhập lại!";
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", msg, response);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File upload too large: {}", ex.getMessage());
        String msg = "Dung lượng file tải lên vượt quá giới hạn cho phép (tối đa 5MB)!";
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "BAD REQUEST", msg, response);
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex) {
        log.error("Internal Server Error: ", ex);
        String msg = "Hệ thống đang gặp sự cố, vui lòng thử lại sau!";
        Map<String, Object> response = new HashMap<>();
        response.put("errors", msg);
        response.put("message", msg);
        ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", msg, response);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
}
