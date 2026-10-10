package com.exam.config.security;

import com.exam.config.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, UserDetailsService userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestTokenHeader = request.getHeader("Authorization");
        String jwtToken = null;
        String username = null;

        // First check cookies
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("coe_auth".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        // Fallback to header if not in cookie
        if (jwtToken == null && requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
        }

        // Fallback to query parameter (for file downloads via window.open)
        if (jwtToken == null && request.getParameter("token") != null) {
            jwtToken = request.getParameter("token");
        }

        if (jwtToken != null) {
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (Exception e) {
                logger.warn("Unable to get JWT Token or Token has expired");
            }
        } else {
            logger.debug("JWT Token not found in cookies or headers");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                
                String tenantId = jwtTokenUtil.getTenantIdFromToken(jwtToken);
                if (tenantId != null) {
                    TenantContext.setCurrentTenant(tenantId);
                }
            }
        }
        
        // Fallback to X-Tenant-ID header if TenantContext was not set by JWT
        String headerTenant = request.getHeader("X-Tenant-ID");
        if (TenantContext.getCurrentTenant() == null && headerTenant != null && !headerTenant.isBlank()) {
            TenantContext.setCurrentTenant(headerTenant.trim());
        }
        
        // Fallback to query parameter (e.g. for window.open file downloads)
        if (TenantContext.getCurrentTenant() == null) {
            String paramTenant = request.getParameter("tenantId") != null ? request.getParameter("tenantId") : request.getParameter("tenant");
            if (paramTenant != null && !paramTenant.isBlank()) {
                TenantContext.setCurrentTenant(paramTenant.trim());
            }
        }
        
        // Default tenant fallback for operational data: all exams, allocations, batches, and duties reside in "krce"
        String path = request.getRequestURI();
        if (TenantContext.getCurrentTenant() == null && (path == null || !path.startsWith("/api/v1/admin"))) {
            TenantContext.setCurrentTenant("krce");
        }
        
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
