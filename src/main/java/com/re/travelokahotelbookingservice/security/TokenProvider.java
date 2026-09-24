package com.re.travelokahotelbookingservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
@Component
public class TokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.accessTokenExpiration}")
    private long accessExpiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(Authentication authentication) {
        return generateAccessTokenFromEmail(authentication.getName());
    }

    public String generateAccessTokenFromEmail(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpiration);
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }


    public String getToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (StringUtils.hasText(bearer) && bearer.startsWith("Bearer "))
                ? bearer.substring(7) : null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public String getSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public Instant getExpiryDate(String accessToken) {
        Claims claims = Jwts.parser().verifyWith(key)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();
        return claims.getExpiration().toInstant();
    }
}
