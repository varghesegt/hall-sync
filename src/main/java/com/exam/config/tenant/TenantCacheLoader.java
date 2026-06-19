package com.exam.config.tenant;

import com.exam.entity.master.Tenant;
import com.exam.repository.master.TenantRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class TenantCacheLoader implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(TenantCacheLoader.class);

    private final TenantRepository tenantRepository;

    public TenantCacheLoader(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            List<Tenant> tenants = tenantRepository.findAll();
            for (Tenant tenant : tenants) {
                TenantContext.registerTenantDetails(tenant.getTenantId(), tenant.getCollegeName(), tenant.getRemunerationRate());
            }
            logger.info("Loaded {} tenants into college cache.", tenants.size());
        } catch (Exception e) {
            logger.error("Failed to pre-load tenant college map: {}", e.getMessage(), e);
        }
    }
}
