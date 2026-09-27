package com.re.travelokahotelbookingservice.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

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
     * - Tên HTTP Method, đường dẫn URI
     * - Class và Method xử lý
     * - Thời gian thực thi (Performance execution time) tính bằng ms
     */
    @Around("controllerPointcut()")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestURI = request != null ? request.getRequestURI() : "UNKNOWN";
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.info("==> [REQUEST] {} {} -> {}.{}()", httpMethod, requestURI, className, methodName);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("<== [RESPONSE] {} {} -> {}.{}() thành công trong [{} ms]", 
                    httpMethod, requestURI, className, methodName, duration);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("<== [RESPONSE ERROR] {} {} -> {}.{}() thất bại sau [{} ms] - Lỗi: {}", 
                    httpMethod, requestURI, className, methodName, duration, ex.getMessage());
            throw ex;
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
}
