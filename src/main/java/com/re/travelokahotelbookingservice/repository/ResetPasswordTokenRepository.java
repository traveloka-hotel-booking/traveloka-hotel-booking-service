package com.re.travelokahotelbookingservice.repository;

import com.re.travelokahotelbookingservice.entity.ResetPasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResetPasswordTokenRepository extends JpaRepository<ResetPasswordToken, Long> {
    Optional<ResetPasswordToken> findByResetToken(String resetToken);

}
