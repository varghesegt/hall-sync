package com.exam.controller;

import com.exam.dto.admin.CreateTenantRequest;
import com.exam.service.master.TenantProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final TenantProvisioningService tenantProvisioningService;

    public AdminController(TenantProvisioningService tenantProvisioningService) {
        this.tenantProvisioningService = tenantProvisioningService;
    }

    @PostMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        tenantProvisioningService.provisionTenant(request);
        return ResponseEntity.ok("Tenant and database provisioned successfully.");
    }
}
