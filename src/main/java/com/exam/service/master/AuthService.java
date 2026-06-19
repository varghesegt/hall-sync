package com.exam.service.master;

import com.exam.config.security.JwtTokenUtil;
import com.exam.dto.auth.AuthRequest;
import com.exam.dto.auth.AuthResponse;
import com.exam.entity.master.AppUser;
import com.exam.entity.master.Tenant;
import com.exam.repository.master.UserRepository;
import com.exam.repository.master.TenantRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService,
                       JwtTokenUtil jwtTokenUtil,
                       UserRepository userRepository,
                       TenantRepository tenantRepository,
                       JavaMailSender mailSender,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse authenticate(AuthRequest request) {
        String inputUsername = request.getEmail();
        String resolvedEmail = inputUsername;

        String previousTenant = com.exam.config.tenant.TenantContext.getCurrentTenant();
        try {
            com.exam.config.tenant.TenantContext.clear();

            // If it's a tenantId (doesn't contain @), resolve it to its administrator email
            if (inputUsername != null && !inputUsername.contains("@")) {
                Optional<Tenant> tenantOpt = tenantRepository.findByTenantId(inputUsername);
                if (tenantOpt.isPresent()) {
                    Tenant tenant = tenantOpt.get();
                    Optional<AppUser> adminOpt = userRepository.findAll().stream()
                            .filter(u -> u.getTenant() != null && u.getTenant().getId().equals(tenant.getId()))
                            .findFirst();
                    if (adminOpt.isPresent()) {
                        resolvedEmail = adminOpt.get().getEmail();
                    }
                }
            }

            final UserDetails userDetails = userDetailsService.loadUserByUsername(resolvedEmail);
            logger.debug(">>> AuthService: Checking password manually via encoder...");
            boolean matches = passwordEncoder.matches(request.getPassword(), userDetails.getPassword());
            logger.debug(">>> AuthService: passwordEncoder.matches() result: {}", matches);

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(resolvedEmail, request.getPassword())
            );

            AppUser appUser = userRepository.findByEmail(resolvedEmail).orElseThrow();
        
        String tenantId = null;
        if (appUser.getTenant() != null) {
            tenantId = appUser.getTenant().getTenantId();
        }

            final String token = jwtTokenUtil.generateToken(userDetails, tenantId);

            return new AuthResponse(token, appUser.getRole(), tenantId);
        } finally {
            if (previousTenant != null) {
                com.exam.config.tenant.TenantContext.setCurrentTenant(previousTenant);
            } else {
                com.exam.config.tenant.TenantContext.clear();
            }
        }
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Transactional
    public void processForgotPassword(String email) {
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            String token = UUID.randomUUID().toString();
            user.setResetToken(hashToken(token));
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); // 1 hour expiry
            userRepository.save(user);

            // Send Email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Password Reset Request - HallSync");
            message.setText("To reset your password, use the following token (this would typically be a link): \n\n" + token);
            
            try {
                mailSender.send(message);
                logger.info(">>> SUCCESS: Password reset email successfully sent to {}", user.getEmail());
            } catch (Exception e) {
                logger.warn(">>> WARNING: Could not send password reset email via SMTP (placeholder values used).");
                logger.info(">>> DEV MODE RESET TOKEN FOR [{}]: {}", user.getEmail(), token);
            }
        }
        // Always return success even if email not found to prevent user enumeration
    }

    @Transactional
    public boolean processResetPassword(String token, String newPassword) {
        Optional<AppUser> userOpt = userRepository.findByResetToken(hashToken(token));
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            if (user.getResetTokenExpiry().isAfter(LocalDateTime.now())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }
}
