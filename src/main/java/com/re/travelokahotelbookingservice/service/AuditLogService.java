package com.re.travelokahotelbookingservice.service;

import com.re.travelokahotelbookingservice.entity.AuditLog;

public interface AuditLogService {

    /**
     * Lưu thông tin nhật ký kiểm toán (AuditLog) vào cơ sở dữ liệu bất đồng bộ trên luồng nền.
     */
    void saveAuditLogAsync(AuditLog auditLog);
}
