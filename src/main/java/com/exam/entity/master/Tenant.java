package com.exam.entity.master;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String tenantId; // e.g. krce, srm

    @Column(nullable = false)
    private String collegeName;

    @Column(nullable = false)
    private String dbName; // the postgres db name e.g. tenant_krce

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "remuneration_rate")
    private Double remunerationRate = 150.0;

    @Column(name = "is_configured", nullable = false)
    private Boolean isConfigured = false;

    @Column(name = "logo_base64", columnDefinition = "TEXT")
    private String logoBase64;

    public Tenant() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Double getRemunerationRate() {
        return remunerationRate;
    }

    public void setRemunerationRate(Double remunerationRate) {
        this.remunerationRate = remunerationRate;
    }

    public Boolean getIsConfigured() {
        return isConfigured;
    }

    public void setIsConfigured(Boolean configured) {
        isConfigured = configured;
    }

    public String getLogoBase64() {
        return logoBase64;
    }

    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }
}
