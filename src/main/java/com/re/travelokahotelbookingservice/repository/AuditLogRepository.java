package com.re.travelokahotelbookingservice.repository;

import com.re.travelokahotelbookingservice.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsernameOrderByCreatedAtDesc(String username);
}
