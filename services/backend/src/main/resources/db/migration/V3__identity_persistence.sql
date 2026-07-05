ALTER TABLE identity.users
    ADD COLUMN password_hash VARCHAR(255),
    ADD COLUMN auth_status VARCHAR(30),
    ADD CONSTRAINT ck_users_auth_status CHECK (
        auth_status IS NULL
        OR auth_status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'SUSPENDED', 'DISABLED')
    );

CREATE TABLE identity.user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES identity.roles (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role ON identity.user_roles (role_id);

CREATE TABLE identity.role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id)
        REFERENCES identity.roles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id)
        REFERENCES identity.permissions (id) ON DELETE RESTRICT
);

CREATE INDEX idx_role_permissions_permission ON identity.role_permissions (permission_id);

CREATE TABLE identity.refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_refresh_tokens_period CHECK (expires_at > issued_at),
    CONSTRAINT ck_refresh_tokens_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_refresh_tokens_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_refresh_tokens_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_refresh_tokens_user_status
    ON identity.refresh_tokens (user_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_refresh_tokens_expires_at
    ON identity.refresh_tokens (expires_at) WHERE is_deleted = FALSE AND status = 'ACTIVE';

CREATE TABLE identity.otp_verifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT fk_otp_verifications_user FOREIGN KEY (user_id)
        REFERENCES identity.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_otp_code CHECK (otp_code ~ '^[0-9]{6}$'),
    CONSTRAINT ck_otp_period CHECK (expires_at > generated_at),
    CONSTRAINT ck_otp_purpose CHECK (
        purpose IN ('REGISTRATION', 'SIGN_IN', 'PASSWORD_RESET', 'MOBILE_CHANGE')
    ),
    CONSTRAINT ck_otp_status CHECK (status IN ('PENDING', 'VERIFIED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_otp_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_otp_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_otp_user_purpose_status
    ON identity.otp_verifications (user_id, purpose, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_otp_expires_at
    ON identity.otp_verifications (expires_at) WHERE is_deleted = FALSE AND status = 'PENDING';
