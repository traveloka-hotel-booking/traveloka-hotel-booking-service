package com.re.travelokahotelbookingservice.service;

public interface TokenBlacklistCleanupService {
    void cleanupExpiredTokens();
}
