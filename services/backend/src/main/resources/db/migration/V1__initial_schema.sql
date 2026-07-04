CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS community;
CREATE SCHEMA IF NOT EXISTS finance;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE identity.users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    given_name VARCHAR(100) NOT NULL,
    family_name VARCHAR(100),
    email VARCHAR(254),
    phone_number VARCHAR(16),
    status VARCHAR(30) NOT NULL,
    language_code VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT uk_users_tenant_phone UNIQUE (tenant_id, phone_number),
    CONSTRAINT ck_users_contact CHECK (email IS NOT NULL OR phone_number IS NOT NULL),
    CONSTRAINT ck_users_phone CHECK (phone_number IS NULL OR phone_number ~ '^\+[1-9][0-9]{7,14}$'),
    CONSTRAINT ck_users_status CHECK (status IN ('INVITED', 'ACTIVE', 'LOCKED', 'SUSPENDED', 'DELETED')),
    CONSTRAINT ck_users_language CHECK (language_code IN ('ENGLISH', 'HINDI', 'MARATHI')),
    CONSTRAINT ck_users_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_users_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_users_tenant_status ON identity.users (tenant_id, status) WHERE is_deleted = FALSE;

CREATE TABLE identity.roles (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    role_code VARCHAR(60) NOT NULL,
    role_name VARCHAR(120) NOT NULL,
    role_scope VARCHAR(30) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_roles_tenant_code UNIQUE (tenant_id, role_code),
    CONSTRAINT ck_roles_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_roles_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_roles_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_roles_platform_code
    ON identity.roles (role_code)
    WHERE tenant_id IS NULL AND is_deleted = FALSE;

CREATE INDEX idx_roles_tenant_status ON identity.roles (tenant_id, status) WHERE is_deleted = FALSE;

CREATE TABLE identity.permissions (
    id UUID PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL,
    module_code VARCHAR(60) NOT NULL,
    action_code VARCHAR(60) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_permissions_code UNIQUE (permission_code),
    CONSTRAINT ck_permissions_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_permissions_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_permissions_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_permissions_module_status
    ON identity.permissions (module_code, status)
    WHERE is_deleted = FALSE;

CREATE TABLE community.groups (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    organizer_user_id UUID NOT NULL,
    group_code VARCHAR(20) NOT NULL,
    group_name VARCHAR(120) NOT NULL,
    module_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    contribution_amount_paise BIGINT NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    duration_cycles INTEGER NOT NULL,
    minimum_members INTEGER NOT NULL,
    maximum_members INTEGER NOT NULL,
    payout_method VARCHAR(30) NOT NULL,
    partial_payment_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_groups_tenant_code UNIQUE (tenant_id, group_code),
    CONSTRAINT fk_groups_organizer FOREIGN KEY (organizer_user_id)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_groups_module_type CHECK (
        module_type IN ('BHISHI', 'SELF_HELP_GROUP', 'SOCIETY_COLLECTION', 'COMMUNITY_FUND')
    ),
    CONSTRAINT ck_groups_status CHECK (
        status IN ('DRAFT', 'PENDING_ACTIVATION', 'ACTIVE', 'SUSPENDED', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT ck_groups_amount CHECK (contribution_amount_paise > 0),
    CONSTRAINT ck_groups_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_groups_frequency CHECK (frequency IN ('WEEKLY', 'MONTHLY', 'QUARTERLY')),
    CONSTRAINT ck_groups_duration CHECK (duration_cycles BETWEEN 1 AND 120),
    CONSTRAINT ck_groups_capacity CHECK (
        minimum_members >= 2 AND maximum_members BETWEEN minimum_members AND 1000
    ),
    CONSTRAINT ck_groups_payout_method CHECK (payout_method IN ('FIXED_ROTATION', 'RANDOM_DRAW', 'AUCTION')),
    CONSTRAINT ck_groups_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_groups_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_groups_tenant_status ON community.groups (tenant_id, status);
CREATE INDEX idx_groups_organizer ON community.groups (organizer_user_id);

CREATE TABLE community.group_members (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    group_id UUID NOT NULL,
    user_id UUID NOT NULL,
    member_number VARCHAR(32) NOT NULL,
    role_in_group VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    exited_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_group_members_group_user UNIQUE (group_id, user_id),
    CONSTRAINT uk_group_members_number UNIQUE (group_id, member_number),
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id)
        REFERENCES community.groups (id) ON DELETE RESTRICT,
    CONSTRAINT fk_group_members_user FOREIGN KEY (user_id)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_group_members_role CHECK (role_in_group IN ('ORGANIZER', 'CO_ORGANIZER', 'MEMBER')),
    CONSTRAINT ck_group_members_status CHECK (status IN ('INVITED', 'ACTIVE', 'PAUSED', 'EXITED', 'REMOVED')),
    CONSTRAINT ck_group_members_exit CHECK (exited_at IS NULL OR exited_at >= joined_at),
    CONSTRAINT ck_group_members_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_group_members_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_group_members_user_status ON community.group_members (user_id, status);
CREATE INDEX idx_group_members_tenant_status ON community.group_members (tenant_id, status);

CREATE TABLE community.monthly_cycles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    group_id UUID NOT NULL,
    cycle_number INTEGER NOT NULL,
    cycle_month DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    opened_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_monthly_cycles_group_number UNIQUE (group_id, cycle_number),
    CONSTRAINT fk_monthly_cycles_group FOREIGN KEY (group_id)
        REFERENCES community.groups (id) ON DELETE RESTRICT,
    CONSTRAINT ck_monthly_cycles_number CHECK (cycle_number >= 1),
    CONSTRAINT ck_monthly_cycles_month CHECK (cycle_month = date_trunc('month', cycle_month)::DATE),
    CONSTRAINT ck_monthly_cycles_status CHECK (
        status IN ('SCHEDULED', 'OPEN', 'COLLECTION_DUE', 'PAYOUT_PENDING', 'CLOSED', 'CANCELLED')
    ),
    CONSTRAINT ck_monthly_cycles_times CHECK (
        (opened_at IS NULL OR opened_at >= created_at)
        AND (closed_at IS NULL OR (opened_at IS NOT NULL AND closed_at >= opened_at))
    ),
    CONSTRAINT ck_monthly_cycles_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_monthly_cycles_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_monthly_cycles_due_status ON community.monthly_cycles (due_date, status);
CREATE INDEX idx_monthly_cycles_tenant_status ON community.monthly_cycles (tenant_id, status);

CREATE TABLE community.installments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    group_id UUID NOT NULL,
    cycle_id UUID NOT NULL,
    group_member_id UUID NOT NULL,
    expected_amount_paise BIGINT NOT NULL,
    paid_amount_paise BIGINT NOT NULL DEFAULT 0,
    penalty_amount_paise BIGINT NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_installments_cycle_member UNIQUE (cycle_id, group_member_id),
    CONSTRAINT fk_installments_group FOREIGN KEY (group_id)
        REFERENCES community.groups (id) ON DELETE RESTRICT,
    CONSTRAINT fk_installments_cycle FOREIGN KEY (cycle_id)
        REFERENCES community.monthly_cycles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_installments_member FOREIGN KEY (group_member_id)
        REFERENCES community.group_members (id) ON DELETE RESTRICT,
    CONSTRAINT ck_installments_amounts CHECK (
        expected_amount_paise >= 0 AND paid_amount_paise >= 0 AND penalty_amount_paise >= 0
    ),
    CONSTRAINT ck_installments_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_installments_status CHECK (
        status IN ('PENDING', 'DUE', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'WAIVED', 'DISPUTED', 'CANCELLED')
    ),
    CONSTRAINT ck_installments_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_installments_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_installments_due_status ON community.installments (due_date, status);
CREATE INDEX idx_installments_member_status ON community.installments (group_member_id, status);
CREATE INDEX idx_installments_group ON community.installments (group_id);

CREATE TABLE community.draws (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    group_id UUID NOT NULL,
    cycle_id UUID NOT NULL,
    selected_group_member_id UUID,
    draw_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    payout_amount_paise BIGINT NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_draws_cycle UNIQUE (cycle_id),
    CONSTRAINT fk_draws_group FOREIGN KEY (group_id)
        REFERENCES community.groups (id) ON DELETE RESTRICT,
    CONSTRAINT fk_draws_cycle FOREIGN KEY (cycle_id)
        REFERENCES community.monthly_cycles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_draws_selected_member FOREIGN KEY (selected_group_member_id)
        REFERENCES community.group_members (id) ON DELETE RESTRICT,
    CONSTRAINT ck_draws_type CHECK (draw_type IN ('RANDOM', 'FIXED_ROTATION', 'AUCTION')),
    CONSTRAINT ck_draws_status CHECK (status IN ('SCHEDULED', 'OPEN', 'COMPLETED', 'CANCELLED', 'DISPUTED')),
    CONSTRAINT ck_draws_amount CHECK (payout_amount_paise >= 0),
    CONSTRAINT ck_draws_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_draws_completion CHECK (completed_at IS NULL OR completed_at >= scheduled_at),
    CONSTRAINT ck_draws_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_draws_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_draws_group_status ON community.draws (group_id, status);
CREATE INDEX idx_draws_selected_member ON community.draws (selected_group_member_id);

CREATE TABLE community.auction_bids (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    group_id UUID NOT NULL,
    draw_id UUID NOT NULL,
    group_member_id UUID NOT NULL,
    bid_amount_paise BIGINT NOT NULL,
    discount_amount_paise BIGINT NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL,
    bid_rank INTEGER,
    status VARCHAR(30) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_auction_bids_draw_member UNIQUE (draw_id, group_member_id),
    CONSTRAINT fk_auction_bids_group FOREIGN KEY (group_id)
        REFERENCES community.groups (id) ON DELETE RESTRICT,
    CONSTRAINT fk_auction_bids_draw FOREIGN KEY (draw_id)
        REFERENCES community.draws (id) ON DELETE RESTRICT,
    CONSTRAINT fk_auction_bids_member FOREIGN KEY (group_member_id)
        REFERENCES community.group_members (id) ON DELETE RESTRICT,
    CONSTRAINT ck_auction_bids_amounts CHECK (bid_amount_paise > 0 AND discount_amount_paise >= 0),
    CONSTRAINT ck_auction_bids_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_auction_bids_rank CHECK (bid_rank IS NULL OR bid_rank >= 1),
    CONSTRAINT ck_auction_bids_status CHECK (
        status IN ('SUBMITTED', 'LEADING', 'OUTBID', 'WITHDRAWN', 'ACCEPTED', 'REJECTED')
    ),
    CONSTRAINT ck_auction_bids_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_auction_bids_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_auction_bids_draw_status ON community.auction_bids (draw_id, status);
CREATE INDEX idx_auction_bids_group ON community.auction_bids (group_id);
CREATE INDEX idx_auction_bids_member ON community.auction_bids (group_member_id);

CREATE TABLE finance.payments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    payer_user_id UUID NOT NULL,
    group_id UUID,
    payment_reference VARCHAR(40) NOT NULL,
    amount_paise BIGINT NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    idempotency_key_hash VARCHAR(128) NOT NULL,
    provider_name VARCHAR(50),
    provider_payment_reference VARCHAR(120),
    reconciliation_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_payments_reference UNIQUE (payment_reference),
    CONSTRAINT uk_payments_tenant_idempotency UNIQUE (tenant_id, idempotency_key_hash),
    CONSTRAINT fk_payments_payer FOREIGN KEY (payer_user_id)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_group FOREIGN KEY (group_id)
        REFERENCES community.groups (id) ON DELETE RESTRICT,
    CONSTRAINT ck_payments_amount CHECK (amount_paise > 0),
    CONSTRAINT ck_payments_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_payments_method CHECK (payment_method IN ('UPI', 'BANK_TRANSFER', 'CARD', 'CASH', 'CHEQUE')),
    CONSTRAINT ck_payments_status CHECK (
        status IN ('INITIATED', 'PENDING_PROVIDER', 'VERIFIED', 'FAILED', 'CANCELLED', 'REFUNDED', 'DISPUTED')
    ),
    CONSTRAINT ck_payments_reconciliation CHECK (
        reconciliation_status IN ('NOT_REQUIRED', 'PENDING', 'MATCHED', 'MISMATCHED', 'RESOLVED')
    ),
    CONSTRAINT ck_payments_provider CHECK (
        (provider_name IS NULL AND provider_payment_reference IS NULL)
        OR (provider_name IS NOT NULL AND provider_payment_reference IS NOT NULL)
    ),
    CONSTRAINT ck_payments_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_payments_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_payments_status ON finance.payments (tenant_id, status);
CREATE INDEX idx_payments_payer ON finance.payments (payer_user_id);
CREATE INDEX idx_payments_group ON finance.payments (group_id);
CREATE INDEX idx_payments_provider_reference
    ON finance.payments (provider_name, provider_payment_reference)
    WHERE provider_payment_reference IS NOT NULL;

CREATE TABLE finance.receipts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    receipt_number VARCHAR(40) NOT NULL,
    receipt_date DATE NOT NULL,
    amount_paise BIGINT NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    delivery_status VARCHAR(30) NOT NULL,
    cancellation_reason VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_receipts_tenant_number UNIQUE (tenant_id, receipt_number),
    CONSTRAINT uk_receipts_payment UNIQUE (payment_id),
    CONSTRAINT fk_receipts_payment FOREIGN KEY (payment_id)
        REFERENCES finance.payments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipts_user FOREIGN KEY (user_id)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_receipts_amount CHECK (amount_paise > 0),
    CONSTRAINT ck_receipts_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_receipts_status CHECK (delivery_status IN ('GENERATED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_receipts_cancel_reason CHECK (
        delivery_status <> 'CANCELLED' OR cancellation_reason IS NOT NULL
    ),
    CONSTRAINT ck_receipts_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_receipts_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_receipts_user_date ON finance.receipts (user_id, receipt_date);

CREATE TABLE notification.notifications (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID,
    notification_type VARCHAR(40) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient_reference VARCHAR(254) NOT NULL,
    subject VARCHAR(160),
    message_body VARCHAR(4000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    priority INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_notifications_type CHECK (
        notification_type IN (
            'VERIFICATION', 'PAYMENT_RECEIPT', 'CONTRIBUTION_REMINDER',
            'GROUP_UPDATE', 'DRAW_RESULT', 'SECURITY_ALERT'
        )
    ),
    CONSTRAINT ck_notifications_channel CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP', 'PUSH')),
    CONSTRAINT ck_notifications_status CHECK (
        status IN ('QUEUED', 'SENDING', 'SENT', 'DELIVERED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT ck_notifications_priority CHECK (priority BETWEEN 0 AND 9),
    CONSTRAINT ck_notifications_sent_at CHECK (sent_at IS NULL OR sent_at >= created_at),
    CONSTRAINT ck_notifications_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_notifications_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_notifications_tenant_status_schedule
    ON notification.notifications (tenant_id, status, scheduled_at);
CREATE INDEX idx_notifications_user_type
    ON notification.notifications (user_id, notification_type);

CREATE TABLE audit.audit_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    actor_user_id UUID,
    actor_type VARCHAR(20) NOT NULL,
    action_code VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID,
    reason_code VARCHAR(100),
    request_id UUID NOT NULL,
    ip_hash VARCHAR(128),
    user_agent_hash VARCHAR(128),
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_audit_logs_actor_type CHECK (actor_type IN ('USER', 'SUPPORT', 'SYSTEM', 'PROVIDER')),
    CONSTRAINT ck_audit_logs_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_audit_logs_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_audit_logs_tenant_occurred ON audit.audit_logs (tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_logs_actor_occurred ON audit.audit_logs (actor_user_id, occurred_at DESC);
CREATE INDEX idx_audit_logs_resource ON audit.audit_logs (resource_type, resource_id);
CREATE INDEX idx_audit_logs_action ON audit.audit_logs (action_code);
