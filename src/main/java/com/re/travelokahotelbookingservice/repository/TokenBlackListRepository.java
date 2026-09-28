package com.re.travelokahotelbookingservice.repository;

import com.re.travelokahotelbookingservice.entity.TokenBlackList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface TokenBlackListRepository extends JpaRepository<TokenBlackList, Long> {

    // Dùng trong AuthenticationFilter để kiểm tra token đã bị logout chưa
    boolean existsByAccessToken(String accessToken);

    // Dọn dẹp các token đã hết hạn khỏi DB để bảng không phình to
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenBlackList t WHERE t.expDate < :now")
    void deleteExpiredTokens(Instant now);
}
