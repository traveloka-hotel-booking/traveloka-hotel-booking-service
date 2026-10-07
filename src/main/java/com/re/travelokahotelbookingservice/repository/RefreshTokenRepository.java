package com.re.travelokahotelbookingservice.repository;

import com.re.travelokahotelbookingservice.entity.RefreshToken;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user.id = :id")
    void deleteByUserId(Long id);

    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
