package com.exam.config.tenant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final Map<String, String> TENANT_COLLEGE_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Double> TENANT_REMUNERATION_MAP = new ConcurrentHashMap<>();

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenant(String tenant) {
        CURRENT_TENANT.set(tenant);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    public static void registerTenantDetails(String tenantId, String collegeName, Double remunerationRate) {
        if (tenantId != null) {
            if (collegeName != null) TENANT_COLLEGE_MAP.put(tenantId, collegeName);
            if (remunerationRate != null) TENANT_REMUNERATION_MAP.put(tenantId, remunerationRate);
        }
    }

    public static String getCollegeName(String tenantId) {
        if (tenantId == null) return null;
        return TENANT_COLLEGE_MAP.get(tenantId);
    }

    public static Double getRemunerationRate(String tenantId) {
        if (tenantId == null) return 150.0;
        return TENANT_REMUNERATION_MAP.getOrDefault(tenantId, 150.0);
    }
}
