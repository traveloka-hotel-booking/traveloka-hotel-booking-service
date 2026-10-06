package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.entity.AuditLog;
import com.re.travelokahotelbookingservice.repository.AuditLogRepository;
import com.re.travelokahotelbookingservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Thực thi lưu nhật ký kiểm toán trên ThreadPool [auditLogExecutor].
     * Sử dụng Propagation.REQUIRES_NEW để đảm bảo giao dịch chạy độc lập,
     * tuyệt đối không ảnh hưởng đến giao dịch của luồng chính.
     */
    @Async("auditLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void saveAuditLogAsync(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
            log.debug("Đã lưu AuditLog thành công trên luồng: [{}] cho endpoint: {}", 
                    Thread.currentThread().getName(), auditLog.getEndpoint());
        } catch (Exception e) {
            log.warn("Không thể lưu AuditLog vào Database (tác vụ ngầm): {}", e.getMessage());
        }
    }
}
