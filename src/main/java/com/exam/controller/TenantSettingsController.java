package com.exam.controller;

import com.exam.config.tenant.TenantContext;
import com.exam.entity.master.Tenant;
import com.exam.repository.master.TenantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/settings")
public class TenantSettingsController {

    private final TenantRepository tenantRepository;

    public TenantSettingsController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings(
            @RequestParam(value = "tenantId", required = false) String paramTenantId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String headerTenantId) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = (paramTenantId != null && !paramTenantId.isBlank()) ? paramTenantId : headerTenantId;
        }
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "krce";
        }
        
        Optional<Tenant> tenantOpt;
        TenantContext.clear();
        try {
            tenantOpt = tenantRepository.findByTenantId(tenantId);
        } finally {
            TenantContext.setCurrentTenant(tenantId);
        }

        if (tenantOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Tenant tenant = tenantOpt.get();
        return ResponseEntity.ok(Map.of(
                "collegeName", tenant.getCollegeName() != null ? tenant.getCollegeName() : "",
                "remunerationRate", tenant.getRemunerationRate() != null ? tenant.getRemunerationRate() : 150.0,
                "isConfigured", tenant.getIsConfigured() != null ? tenant.getIsConfigured() : false
        ));
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> getLogo(
            @RequestParam(value = "tenantId", required = false) String paramTenantId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String headerTenantId) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = (paramTenantId != null && !paramTenantId.isBlank()) ? paramTenantId : headerTenantId;
        }
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "krce";
        }
        
        Optional<Tenant> tenantOpt;
        TenantContext.clear();
        try {
            tenantOpt = tenantRepository.findByTenantId(tenantId);
        } finally {
            TenantContext.setCurrentTenant(tenantId);
        }

        if (tenantOpt.isEmpty() || tenantOpt.get().getLogoBase64() == null || tenantOpt.get().getLogoBase64().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String base64 = tenantOpt.get().getLogoBase64();
        // Base64 string could be in format: data:image/png;base64,iVBORw0KGgo...
        String[] parts = base64.split(",");
        String imageString = parts.length > 1 ? parts[1] : parts[0];
        byte[] imageBytes = java.util.Base64.getDecoder().decode(imageString);

        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=86400")
                .header("Content-Type", base64.contains("image/jpeg") ? "image/jpeg" : "image/png")
                .body(imageBytes);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> payload) {
        String tenantId = TenantContext.getCurrentTenant();
        
        Optional<Tenant> tenantOpt;
        TenantContext.clear();
        try {
            tenantOpt = tenantRepository.findByTenantId(tenantId);
        } finally {
            TenantContext.setCurrentTenant(tenantId);
        }

        if (tenantOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Tenant tenant = tenantOpt.get();

        if (payload.containsKey("collegeName")) {
            tenant.setCollegeName((String) payload.get("collegeName"));
        }
        if (payload.containsKey("remunerationRate")) {
            Object rate = payload.get("remunerationRate");
            if (rate instanceof Number) {
                tenant.setRemunerationRate(((Number) rate).doubleValue());
            } else if (rate instanceof String) {
                tenant.setRemunerationRate(Double.parseDouble((String) rate));
            }
        }
        if (payload.containsKey("logoBase64")) {
            tenant.setLogoBase64((String) payload.get("logoBase64"));
        }
        if (payload.containsKey("isConfigured")) {
            Object configured = payload.get("isConfigured");
            if (configured instanceof Boolean) {
                tenant.setIsConfigured((Boolean) configured);
            } else if (configured instanceof String) {
                tenant.setIsConfigured(Boolean.parseBoolean((String) configured));
            }
        }

        TenantContext.clear();
        try {
            tenant = tenantRepository.save(tenant);
        } finally {
            TenantContext.setCurrentTenant(tenantId);
        }
        
        // Update Cache
        TenantContext.registerTenantDetails(tenant.getTenantId(), tenant.getCollegeName(), tenant.getRemunerationRate());

        return ResponseEntity.ok(Map.of(
                "collegeName", tenant.getCollegeName() != null ? tenant.getCollegeName() : "",
                "remunerationRate", tenant.getRemunerationRate() != null ? tenant.getRemunerationRate() : 150.0,
                "isConfigured", tenant.getIsConfigured() != null ? tenant.getIsConfigured() : false
        ));
    }
}
