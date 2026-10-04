package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.repository.TokenBlackListRepository;
import com.re.travelokahotelbookingservice.service.TokenBlacklistCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class TokenBlackListCleanupServiceImpl implements TokenBlacklistCleanupService {
    private final TokenBlackListRepository tokenBlackListRepository;

    @Scheduled(fixedRate = 3600000)
    @Transactional
    @Override
    public void cleanupExpiredTokens() {
        tokenBlackListRepository.deleteExpiredTokens(Instant.now());
    }

}
