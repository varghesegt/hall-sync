-- V10: Add Remuneration Rate to Tenants
ALTER TABLE tenants ADD COLUMN remuneration_rate DOUBLE PRECISION DEFAULT 150.0;
