-- V6: Add Multi-Tenant tables to Master Database

CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL UNIQUE,
    college_name VARCHAR(255) NOT NULL,
    db_name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    tenant_id UUID,
    reset_token VARCHAR(255),
    reset_token_expiry TIMESTAMP,
    CONSTRAINT fk_app_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);
