package com.re.travelokahotelbookingservice.security;

import com.re.travelokahotelbookingservice.entity.Gender;
import com.re.travelokahotelbookingservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(user.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getFullName() {
        return user.getFullName();
    }

    public LocalDate getYob() { return user.getYob(); }

    public Gender getGender() { return user.getGender(); }

    public String getAddress() { return user.getAddress(); }

    public String getPhone() {
        return user.getPhone();
    }

    public LocalDateTime getCreatedAt() {
        return user.getCreatedAt();
    }

    @Override
    public boolean isEnabled() {
        return user.getIsEnabled();
    }
}
