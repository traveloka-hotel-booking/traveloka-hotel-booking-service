package com.re.travelokahotelbookingservice.repository;

import com.re.travelokahotelbookingservice.entity.TokenBlackList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenBlackListRepository extends JpaRepository<TokenBlackList, Long> {
}
