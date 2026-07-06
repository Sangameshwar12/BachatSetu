ALTER TABLE community.groups
    ADD COLUMN description TEXT;

UPDATE community.groups
SET description = ''
WHERE description IS NULL;

ALTER TABLE community.groups
    ALTER COLUMN description SET NOT NULL,
    ALTER COLUMN group_name TYPE VARCHAR(100);

UPDATE community.groups
SET status = CASE status
    WHEN 'DRAFT' THEN 'INACTIVE'
    WHEN 'PENDING_ACTIVATION' THEN 'INACTIVE'
    WHEN 'COMPLETED' THEN 'CLOSED'
    WHEN 'CANCELLED' THEN 'CLOSED'
    ELSE status
END;

ALTER TABLE community.groups
    DROP CONSTRAINT ck_groups_status,
    DROP CONSTRAINT ck_groups_capacity,
    DROP CONSTRAINT uk_groups_tenant_code;

ALTER TABLE community.groups
    ADD CONSTRAINT ck_groups_status CHECK (
        status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'SUSPENDED')
    ),
    ADD CONSTRAINT ck_groups_capacity CHECK (
        minimum_members >= 2
        AND maximum_members BETWEEN minimum_members AND 500
    );

CREATE UNIQUE INDEX uk_groups_tenant_code
    ON community.groups (tenant_id, group_code)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_groups_tenant_active_created
    ON community.groups (tenant_id, created_at, id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_group_members_group_active_joined
    ON community.group_members (group_id, joined_at, id)
    WHERE is_deleted = FALSE;

CREATE TABLE community.membership_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    group_id UUID NOT NULL,
    group_member_id UUID NOT NULL,
    member_id UUID NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_membership_history_event UNIQUE (group_member_id, event_type),
    CONSTRAINT fk_membership_history_group FOREIGN KEY (group_id)
        REFERENCES community.groups (id) ON DELETE RESTRICT,
    CONSTRAINT fk_membership_history_membership FOREIGN KEY (group_member_id)
        REFERENCES community.group_members (id) ON DELETE RESTRICT,
    CONSTRAINT fk_membership_history_member FOREIGN KEY (member_id)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_membership_history_event CHECK (event_type IN ('JOINED', 'REMOVED')),
    CONSTRAINT ck_membership_history_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_membership_history_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL)
    )
);

CREATE INDEX idx_membership_history_group_time
    ON community.membership_history (group_id, occurred_at);

CREATE INDEX idx_membership_history_member_time
    ON community.membership_history (member_id, occurred_at);

CREATE INDEX idx_membership_history_tenant_event
    ON community.membership_history (tenant_id, event_type, occurred_at);

INSERT INTO community.membership_history (
    id,
    tenant_id,
    group_id,
    group_member_id,
    member_id,
    event_type,
    occurred_at,
    created_at,
    created_by,
    updated_at,
    updated_by,
    version,
    is_deleted
)
SELECT
    md5(member.id::TEXT || ':JOINED')::UUID,
    member.tenant_id,
    member.group_id,
    member.id,
    member.user_id,
    'JOINED',
    member.joined_at,
    member.joined_at,
    member.user_id,
    member.joined_at,
    member.user_id,
    0,
    FALSE
FROM community.group_members member
ON CONFLICT (group_member_id, event_type) DO NOTHING;

INSERT INTO community.membership_history (
    id,
    tenant_id,
    group_id,
    group_member_id,
    member_id,
    event_type,
    occurred_at,
    created_at,
    created_by,
    updated_at,
    updated_by,
    version,
    is_deleted
)
SELECT
    md5(member.id::TEXT || ':REMOVED')::UUID,
    member.tenant_id,
    member.group_id,
    member.id,
    member.user_id,
    'REMOVED',
    member.exited_at,
    member.exited_at,
    member.user_id,
    member.exited_at,
    member.user_id,
    0,
    FALSE
FROM community.group_members member
WHERE member.exited_at IS NOT NULL
ON CONFLICT (group_member_id, event_type) DO NOTHING;
