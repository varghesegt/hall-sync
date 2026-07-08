package com.exam.controller;

import com.exam.dto.auth.AuthRequest;
import com.exam.dto.auth.AuthResponse;
import com.exam.dto.auth.ForgotPasswordRequest;
import com.exam.dto.auth.ResetPasswordRequest;
import com.exam.service.master.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @org.springframework.beans.factory.annotation.Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, jakarta.servlet.http.HttpServletResponse response) {
        StringBuilder hex = new StringBuilder();
        for (char c : request.getPassword().toCharArray()) {
            hex.append(String.format("%04x ", (int) c));
        }
        logger.debug(">>> Login attempt for email: '{}', password HEX: {}", request.getEmail(), hex.toString());
        AuthResponse authResponse = authService.authenticate(request);
        
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("coe_auth", authResponse.getToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // 24 hours
        // Set SameSite=Lax via header for cross-site protection
        String sameSite = cookieSecure ? "Strict" : "Lax";
        String secureFlag = cookieSecure ? " Secure;" : "";
        response.addHeader("Set-Cookie", "coe_auth=" + authResponse.getToken() + "; Max-Age=86400; Path=/; HttpOnly;" + secureFlag + " SameSite=" + sameSite);
        
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletResponse response) {
        response.addHeader("Set-Cookie", "coe_auth=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.processForgotPassword(request.getEmail());
        return ResponseEntity.ok("If an account exists with that email, a password reset token has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean success = authService.processResetPassword(request.getToken(), request.getNewPassword());
        if (success) {
            return ResponseEntity.ok("Password has been reset successfully.");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired reset token.");
        }
    }
}
