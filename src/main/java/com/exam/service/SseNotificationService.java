package com.exam.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SseNotificationService.class);
    
    // Map of tenantId -> Map of emitterId -> SseEmitter
    private final Map<String, Map<String, SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String tenantId, String emitterId) {
        SseEmitter emitter = new SseEmitter(3600000L); // 1 hour timeout
        
        tenantEmitters.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>()).put(emitterId, emitter);
        
        emitter.onCompletion(() -> removeEmitter(tenantId, emitterId));
        emitter.onTimeout(() -> removeEmitter(tenantId, emitterId));
        emitter.onError((e) -> removeEmitter(tenantId, emitterId));
        
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to SSE stream for tenant: " + tenantId));
        } catch (IOException e) {
            removeEmitter(tenantId, emitterId);
        }
        
        return emitter;
    }

    private void removeEmitter(String tenantId, String emitterId) {
        Map<String, SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters != null) {
            emitters.remove(emitterId);
            if (emitters.isEmpty()) {
                tenantEmitters.remove(tenantId);
            }
        }
    }

    public void broadcastToTenant(String tenantId, String eventName, Object data) {
        Map<String, SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters != null) {
            emitters.forEach((id, emitter) -> {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                    removeEmitter(tenantId, id);
                }
            });
        }
    }
}
