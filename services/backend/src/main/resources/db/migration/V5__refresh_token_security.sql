ALTER TABLE identity.refresh_tokens
    DROP CONSTRAINT ck_refresh_tokens_status,
    ADD COLUMN tenant_id UUID,
    ADD COLUMN session_id UUID,
    ADD COLUMN token_hash VARCHAR(255),
    ADD COLUMN replaced_by_token_id UUID;

UPDATE identity.refresh_tokens AS token
SET tenant_id = users.tenant_id,
    session_id = token.id,
    token_hash = 'legacy-revoked-' || REPLACE(token.id::TEXT, '-', ''),
    status = 'REVOKED'
FROM identity.users AS users
WHERE users.id = token.user_id;

ALTER TABLE identity.refresh_tokens
    ALTER COLUMN tenant_id SET NOT NULL,
    ALTER COLUMN session_id SET NOT NULL,
    ALTER COLUMN token_hash SET NOT NULL,
    ADD CONSTRAINT fk_refresh_tokens_replacement FOREIGN KEY (replaced_by_token_id)
        REFERENCES identity.refresh_tokens (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_refresh_tokens_status CHECK (
        status IN ('ACTIVE', 'REVOKED', 'EXPIRED', 'ROTATED', 'REUSED')
    ),
    ADD CONSTRAINT ck_refresh_tokens_hash CHECK (CHAR_LENGTH(token_hash) BETWEEN 32 AND 255),
    ADD CONSTRAINT ck_refresh_tokens_replacement CHECK (
        (status IN ('ROTATED', 'REUSED') AND replaced_by_token_id IS NOT NULL)
        OR (status NOT IN ('ROTATED', 'REUSED') AND replaced_by_token_id IS NULL)
    );

CREATE UNIQUE INDEX uk_refresh_tokens_active_session
    ON identity.refresh_tokens (user_id, session_id)
    WHERE is_deleted = FALSE AND status = 'ACTIVE';

CREATE INDEX idx_refresh_tokens_tenant_user
    ON identity.refresh_tokens (tenant_id, user_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_refresh_tokens_replacement
    ON identity.refresh_tokens (replaced_by_token_id)
    WHERE replaced_by_token_id IS NOT NULL;
