CREATE SCHEMA IF NOT EXISTS support;

CREATE TABLE support.support_tickets (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    raised_by UUID NOT NULL,
    category VARCHAR(20) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    assigned_to UUID,
    resolved_at TIMESTAMPTZ,
    resolution TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT ck_support_tickets_category CHECK (
        category IN ('LOGIN', 'OTP', 'PAYMENT', 'GROUP', 'DRAW', 'NOTIFICATION', 'RECEIPT', 'STORAGE', 'OTHER')
    ),
    CONSTRAINT ck_support_tickets_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_support_tickets_status CHECK (
        status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')
    ),
    CONSTRAINT ck_support_tickets_resolution_pair CHECK (
        (status IN ('RESOLVED', 'CLOSED') AND resolved_at IS NOT NULL AND resolution IS NOT NULL)
        OR (status NOT IN ('RESOLVED', 'CLOSED') AND resolved_at IS NULL AND resolution IS NULL)
    )
);

CREATE INDEX idx_support_tickets_tenant_status ON support.support_tickets (tenant_id, status);
CREATE INDEX idx_support_tickets_raised_by ON support.support_tickets (raised_by);

CREATE SCHEMA IF NOT EXISTS platform;

CREATE TABLE platform.tenants (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    suspension_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT ck_tenants_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

CREATE TABLE platform.announcements (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    severity VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT ck_announcements_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT ck_announcements_window CHECK (end_at >= start_at)
);

CREATE INDEX idx_announcements_window ON platform.announcements (start_at, end_at);

ALTER TABLE audit.audit_entries DROP CONSTRAINT ck_audit_entries_event_type;

ALTER TABLE audit.audit_entries ADD CONSTRAINT ck_audit_entries_event_type CHECK (event_type IN (
    'LOGIN', 'LOGOUT', 'OTP_SENT', 'OTP_VERIFIED',
    'GROUP_CREATED', 'GROUP_UPDATED', 'GROUP_CLOSED',
    'MEMBER_ADDED', 'MEMBER_REMOVED',
    'PAYMENT_CREATED', 'PAYMENT_VERIFIED', 'PAYMENT_REFUNDED',
    'DRAW_CREATED', 'DRAW_COMPLETED',
    'RECEIPT_GENERATED', 'PDF_DOWNLOADED',
    'NOTIFICATION_SENT',
    'FILE_UPLOADED', 'FILE_DELETED',
    'GATEWAY_REFUND_INITIATED', 'GATEWAY_WEBHOOK_PROCESSED',
    'ADMIN_ANALYTICS_VIEWED',
    'PLATFORM_CONFIGURATION_UPDATED', 'FEATURE_FLAG_UPDATED', 'SYSTEM_LIMIT_UPDATED',
    'USER_REGISTERED', 'PROFILE_COMPLETED',
    'INVITATION_CREATED', 'INVITATION_REVOKED',
    'GROUP_JOINED', 'QR_JOINED', 'LINK_JOINED',
    'TENANT_SUSPENDED', 'TENANT_ACTIVATED', 'TENANT_ARCHIVED',
    'SUPPORT_TICKET_CREATED', 'SUPPORT_TICKET_ASSIGNED', 'SUPPORT_TICKET_RESOLVED', 'SUPPORT_TICKET_CLOSED',
    'ANNOUNCEMENT_PUBLISHED', 'BROADCAST_NOTIFICATION_SENT',
    'SYSTEM_EVENT'
));
