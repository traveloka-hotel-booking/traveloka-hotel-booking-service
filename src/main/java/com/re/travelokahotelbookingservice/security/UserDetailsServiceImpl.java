package com.re.travelokahotelbookingservice.security;

import com.re.travelokahotelbookingservice.entity.User;
import com.re.travelokahotelbookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;


    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {


        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not fount"));
        System.out.println("EMAIL FROM LOGIN = " + email);
        System.out.println("USER FOUND = " + user.getEmail());
        return new UserDetailsImpl(user);
    }
}
