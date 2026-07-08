package com.exam.controller;

import com.exam.config.tenant.TenantContext;
import com.exam.service.SseNotificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stream")
public class SseController {

    private final SseNotificationService sseNotificationService;

    public SseController(SseNotificationService sseNotificationService) {
        this.sseNotificationService = sseNotificationService;
    }

    @GetMapping(path = "/allocations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAllocations() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new RuntimeException("Missing tenant context for SSE stream");
        }
        
        // Generate a unique ID for this client connection
        String emitterId = UUID.randomUUID().toString();
        
        return sseNotificationService.subscribe(tenantId, emitterId);
    }
}
