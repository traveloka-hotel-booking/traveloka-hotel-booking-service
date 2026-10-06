package com.re.travelokahotelbookingservice.aop;

import com.re.travelokahotelbookingservice.entity.AuditLog;
import com.re.travelokahotelbookingservice.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private final AuditLogRepository auditLogRepository;

    /**
     * Pointcut bao quát toàn bộ Controller trong hệ thống
     */
    @Pointcut("within(com.re.travelokahotelbookingservice.controller..*)")
    public void controllerPointcut() {}

    /**
     * Pointcut bao quát toàn bộ Service Implementation trong hệ thống
     */
    @Pointcut("within(com.re.travelokahotelbookingservice.service.impl..*)")
    public void servicePointcut() {}

    /**
     * Ghi log tự động khi có Request gọi vào bất kỳ Controller nào:
     * 1. Ghi log ra Console (Application Logging)
     * 2. Tự động lưu bản ghi vào bảng audit_logs trong MySQL (Audit Logging)
     */
    @Around("controllerPointcut()")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestURI = request != null ? request.getRequestURI() : "UNKNOWN";
        String ipAddress = getClientIp(request);
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String actionName = className + "." + methodName;

        log.info("==> [REQUEST] {} {} -> {}() | IP: {}", httpMethod, requestURI, actionName, ipAddress);

        Object result;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("<== [RESPONSE] {} {} -> {}() thành công trong [{} ms]", 
                    httpMethod, requestURI, actionName, duration);
            return result;
        } catch (Throwable ex) {
            status = "FAILED";
            errorMessage = ex.getMessage();
            long duration = System.currentTimeMillis() - startTime;
            log.error("<== [RESPONSE ERROR] {} {} -> {}() thất bại sau [{} ms] - Lỗi: {}", 
                    httpMethod, requestURI, actionName, duration, errorMessage);
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            saveAuditLog(httpMethod, requestURI, actionName, ipAddress, status, errorMessage, duration);
        }
    }

    /**
     * Ghi log cảnh báo khi có Exception xảy ra ở tầng Service
     */
    @AfterThrowing(pointcut = "servicePointcut()", throwing = "e")
    public void logAfterThrowingService(JoinPoint joinPoint, Throwable e) {
        log.error("💥 [SERVICE EXCEPTION] Tại {}.{}() - Nguyên nhân: {}",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                e.getMessage());
    }

    /**
     * Lưu nhật ký vào bảng audit_logs trong MySQL (Bọc try-catch an toàn để không ảnh hưởng luồng chính)
     */
    private void saveAuditLog(String httpMethod, String requestURI, String actionName, 
                              String ipAddress, String status, String errorMessage, long duration) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) 
                    ? auth.getName() : "ANONYMOUS";

            AuditLog auditLog = AuditLog.builder()
                    .username(username)
                    .action(actionName)
                    .endpoint(requestURI)
                    .httpMethod(httpMethod)
                    .ipAddress(ipAddress)
                    .status(status)
                    .errorMessage(errorMessage)
                    .executionTimeMs(duration)
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Không thể lưu AuditLog vào Database: {}", e.getMessage());
        }
    }

    /**
     * Lấy địa chỉ IP chính xác của Client (kể cả khi qua Proxy/Load Balancer)
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        String resolvedIp = (ip != null && ip.contains(",")) ? ip.split(",")[0].trim() : ip;
        if ("0:0:0:0:0:0:0:1".equals(resolvedIp) || "::1".equals(resolvedIp)) {
            return "127.0.0.1";
        }
        return resolvedIp;
    }
}
