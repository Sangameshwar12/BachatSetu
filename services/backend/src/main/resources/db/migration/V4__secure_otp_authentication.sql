CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE identity.otp_verifications
    ADD COLUMN otp_hash VARCHAR(255),
    ADD COLUMN verification_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN resend_count INTEGER NOT NULL DEFAULT 0;

UPDATE identity.otp_verifications
SET otp_hash = crypt(otp_code, gen_salt('bf', 12));

ALTER TABLE identity.otp_verifications
    ALTER COLUMN otp_hash SET NOT NULL,
    DROP CONSTRAINT ck_otp_code,
    DROP CONSTRAINT ck_otp_status,
    DROP COLUMN otp_code,
    ADD CONSTRAINT ck_otp_hash CHECK (
        char_length(otp_hash) BETWEEN 32 AND 255
        AND otp_hash !~ '^[0-9]{6}$'
    ),
    ADD CONSTRAINT ck_otp_verification_attempts CHECK (verification_attempts BETWEEN 0 AND 5),
    ADD CONSTRAINT ck_otp_resend_count CHECK (resend_count BETWEEN 0 AND 3),
    ADD CONSTRAINT ck_otp_status CHECK (
        status IN ('PENDING', 'VERIFIED', 'FAILED', 'EXPIRED', 'INVALIDATED')
    );

CREATE UNIQUE INDEX uk_otp_active_user_purpose
    ON identity.otp_verifications (user_id, purpose)
    WHERE status = 'PENDING' AND is_deleted = FALSE;
