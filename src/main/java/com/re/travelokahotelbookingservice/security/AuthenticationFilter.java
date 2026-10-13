package com.re.travelokahotelbookingservice.security;

import com.re.travelokahotelbookingservice.repository.TokenBlackListRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlackListRepository tokenBlackListRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Optional.ofNullable(tokenProvider.getToken(request))
                    .filter(StringUtils::hasText)
                    // Nếu token đã bị logout → từ chối, không cho đi tiếp
                    .filter(token -> !tokenBlackListRepository.existsByAccessToken(token))
                    .filter(tokenProvider::validateToken)
                    .map(tokenProvider::getSubject)
                    .map(userDetailsService::loadUserByUsername)
                    //So sánh tokenVersion trước khi tạo Authenticaion
                    .filter(userDetails -> {
                        String token = tokenProvider.getToken(request);
                        int jwtVersion = tokenProvider.getTokenVersion(token);
                        if(!(userDetails instanceof  UserDetailsImpl customUserDetails)) {
                            return false;
                        }
                        int currentVersion = customUserDetails.getTokenVersion();
                        return jwtVersion == currentVersion;
                    })
                    .map(userDetails -> {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        return auth;
                    })
                    .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));

        } catch (Exception e) {
            log.error("Could not set user authentication in security context: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
