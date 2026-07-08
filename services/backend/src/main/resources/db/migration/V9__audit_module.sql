CREATE TABLE audit.audit_entries (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    actor_id UUID,
    event_type VARCHAR(40) NOT NULL,
    module_name VARCHAR(60) NOT NULL,
    resource_type VARCHAR(60),
    resource_id UUID,
    action VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    ip_address VARCHAR(64),
    user_agent VARCHAR(255),
    metadata JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT ck_audit_entries_event_type CHECK (event_type IN (
        'LOGIN', 'LOGOUT', 'OTP_SENT', 'OTP_VERIFIED',
        'GROUP_CREATED', 'GROUP_UPDATED', 'GROUP_CLOSED',
        'MEMBER_ADDED', 'MEMBER_REMOVED',
        'PAYMENT_CREATED', 'PAYMENT_VERIFIED', 'PAYMENT_REFUNDED',
        'DRAW_CREATED', 'DRAW_COMPLETED',
        'RECEIPT_GENERATED', 'PDF_DOWNLOADED',
        'NOTIFICATION_SENT',
        'FILE_UPLOADED', 'FILE_DELETED',
        'GATEWAY_REFUND_INITIATED', 'GATEWAY_WEBHOOK_PROCESSED',
        'SYSTEM_EVENT'
    )),
    CONSTRAINT ck_audit_entries_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_audit_entries_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_audit_entries_tenant_id ON audit.audit_entries (tenant_id);
CREATE INDEX idx_audit_entries_actor_id ON audit.audit_entries (actor_id);
CREATE INDEX idx_audit_entries_module_name ON audit.audit_entries (module_name);
CREATE INDEX idx_audit_entries_event_type ON audit.audit_entries (event_type);
CREATE INDEX idx_audit_entries_created_at ON audit.audit_entries (occurred_at);
