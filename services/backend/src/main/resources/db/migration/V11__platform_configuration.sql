CREATE SCHEMA IF NOT EXISTS config;

CREATE TABLE config.platform_configuration (
    id SMALLINT PRIMARY KEY DEFAULT 1,
    default_language VARCHAR(10) NOT NULL,
    otp_expiry_seconds INTEGER NOT NULL CHECK (otp_expiry_seconds > 0),
    default_storage_provider VARCHAR(50) NOT NULL,
    default_payment_provider VARCHAR(50) NOT NULL,
    notification_retry_count INTEGER NOT NULL CHECK (notification_retry_count >= 0),
    maximum_upload_size_bytes BIGINT NOT NULL CHECK (maximum_upload_size_bytes > 0),
    maximum_members_per_group INTEGER NOT NULL CHECK (maximum_members_per_group > 0),
    maximum_groups_per_organizer INTEGER NOT NULL CHECK (maximum_groups_per_organizer > 0),
    maintenance_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    maintenance_message TEXT,
    maintenance_start_at TIMESTAMPTZ,
    maintenance_end_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    CONSTRAINT ck_platform_configuration_singleton CHECK (id = 1)
);

CREATE TABLE config.feature_flags (
    feature_key VARCHAR(50) PRIMARY KEY CHECK (feature_key IN (
        'AUTHENTICATION', 'PAYMENTS', 'NOTIFICATIONS', 'STORAGE', 'RECEIPTS',
        'AUCTION', 'ANALYTICS', 'AUDIT', 'SIGNUP'
    )),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID
);

CREATE TABLE config.platform_limits (
    limit_key VARCHAR(50) PRIMARY KEY CHECK (limit_key IN (
        'MAX_GROUPS', 'MAX_MEMBERS', 'MAX_UPLOADS', 'MAX_RECEIPTS', 'MAX_NOTIFICATIONS'
    )),
    limit_value BIGINT NOT NULL CHECK (limit_value > 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID
);

INSERT INTO config.platform_configuration (
    id, default_language, otp_expiry_seconds, default_storage_provider, default_payment_provider,
    notification_retry_count, maximum_upload_size_bytes, maximum_members_per_group,
    maximum_groups_per_organizer, maintenance_enabled, maintenance_message, maintenance_start_at,
    maintenance_end_at, version, updated_at, updated_by
) VALUES (
    1, 'ENGLISH', 300, 'LOCAL', 'RAZORPAY', 3, 10485760, 100, 20, FALSE, NULL, NULL, NULL, 0,
    '2026-01-01T00:00:00Z', NULL
) ON CONFLICT (id) DO NOTHING;

INSERT INTO config.feature_flags (feature_key, enabled, version, updated_at, updated_by) VALUES
    ('AUTHENTICATION', TRUE, 0, '2026-01-01T00:00:00Z', NULL),
    ('PAYMENTS', TRUE, 0, '2026-01-01T00:00:00Z', NULL),
    ('NOTIFICATIONS', TRUE, 0, '2026-01-01T00:00:00Z', NULL),
    ('STORAGE', TRUE, 0, '2026-01-01T00:00:00Z', NULL),
    ('RECEIPTS', TRUE, 0, '2026-01-01T00:00:00Z', NULL),
    ('AUCTION', TRUE, 0, '2026-01-01T00:00:00Z', NULL),
    ('ANALYTICS', TRUE, 0, '2026-01-01T00:00:00Z', NULL),
    ('AUDIT', TRUE, 0, '2026-01-01T00:00:00Z', NULL),
    ('SIGNUP', TRUE, 0, '2026-01-01T00:00:00Z', NULL)
ON CONFLICT (feature_key) DO NOTHING;

INSERT INTO config.platform_limits (limit_key, limit_value, version, updated_at, updated_by) VALUES
    ('MAX_GROUPS', 100000, 0, '2026-01-01T00:00:00Z', NULL),
    ('MAX_MEMBERS', 1000000, 0, '2026-01-01T00:00:00Z', NULL),
    ('MAX_UPLOADS', 1000000, 0, '2026-01-01T00:00:00Z', NULL),
    ('MAX_RECEIPTS', 1000000, 0, '2026-01-01T00:00:00Z', NULL),
    ('MAX_NOTIFICATIONS', 5000000, 0, '2026-01-01T00:00:00Z', NULL)
ON CONFLICT (limit_key) DO NOTHING;

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
    'SYSTEM_EVENT'
));
