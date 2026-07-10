CREATE TABLE community.group_invitations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    group_id UUID NOT NULL,
    invitation_code VARCHAR(12) NOT NULL,
    secure_token VARCHAR(64) NOT NULL,
    invitation_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    accepted_by UUID,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_group_invitations_tenant_code UNIQUE (tenant_id, invitation_code),
    CONSTRAINT uk_group_invitations_token UNIQUE (secure_token),
    CONSTRAINT fk_group_invitations_group FOREIGN KEY (group_id)
        REFERENCES community.groups (id) ON DELETE RESTRICT,
    CONSTRAINT fk_group_invitations_accepted_by FOREIGN KEY (accepted_by)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_group_invitations_type CHECK (invitation_type IN ('QR', 'CODE', 'LINK')),
    CONSTRAINT ck_group_invitations_status CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_group_invitations_accepted_pair CHECK (
        (accepted_at IS NULL AND accepted_by IS NULL)
        OR (accepted_at IS NOT NULL AND accepted_by IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_group_invitations_active_per_group
    ON community.group_invitations (group_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_group_invitations_tenant_group
    ON community.group_invitations (tenant_id, group_id);
