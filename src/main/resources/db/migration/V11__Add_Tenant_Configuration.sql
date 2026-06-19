-- V11: Add configuration status and logo to tenants

ALTER TABLE tenants 
ADD COLUMN is_configured BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN logo_base64 TEXT;

-- Update existing tenants to be configured so they don't get locked out unexpectedly if they already existed
UPDATE tenants SET is_configured = TRUE;
