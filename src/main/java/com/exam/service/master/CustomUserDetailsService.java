package com.exam.service.master;

import com.exam.entity.master.AppUser;
import com.exam.repository.master.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug(">>> CustomUserDetailsService attempting to load: {}", email);
        AppUser appUser = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn(">>> FAILURE: User not found in database: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
        logger.debug(">>> SUCCESS: Found user in DB: {}", appUser.getEmail());

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(appUser.getRole()));

        return new User(appUser.getEmail(), appUser.getPassword(), authorities);
    }
}
