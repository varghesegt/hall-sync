package com.exam.dto.auth;

public class AuthResponse {
    private String token;
    private String role;
    private String tenantId;

    public AuthResponse(String token, String role, String tenantId) {
        this.token = token;
        this.role = role;
        this.tenantId = tenantId;
    }

    public String getToken() { return token; }
    public String getRole() { return role; }
    public String getTenantId() { return tenantId; }
}
