CREATE SCHEMA IF NOT EXISTS storage;

CREATE TABLE storage.stored_files (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    size BIGINT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT ck_stored_files_provider CHECK (provider IN ('LOCAL', 'AWS_S3', 'AZURE_BLOB', 'GOOGLE_CLOUD_STORAGE')),
    CONSTRAINT ck_stored_files_size CHECK (size >= 0),
    CONSTRAINT ck_stored_files_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_stored_files_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_stored_files_tenant ON storage.stored_files (tenant_id);
