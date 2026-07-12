-- V900 never set created_by/updated_by on any of the rows it inserted. JpaMappingSupport.
-- auditInfo() (used by every JPA mapper, not just auth — the exact same gap V16 already
-- backfilled for the roles/permissions seed data) throws PersistenceMappingException whenever
-- one of these rows is read, which in practice means every seeded user/group/payment/receipt/
-- draw is completely unusable: the app 500s the moment it tries to load any of them. Backfills
-- the same placeholder system-actor UUID V16 used.
--
-- V900's three sample users are additionally missing password_hash/auth_status —
-- AuthUserJpaMapper.toDomain() requires both non-null — making them unable to authenticate at
-- all. The password hash is a random, unusable value exactly like the one
-- StartSignupApplicationService generates for a real signup (see PasswordHashGeneratorPort);
-- this application is OTP-only, so the value is never checked, only its presence matters.
--
-- V900 also gave the three users +911234567890/91/92, which fail MobileNumber's own
-- "+91[6-9]XXXXXXXXX" invariant (a real Indian mobile never starts with 1) — the same rule the
-- signup form enforces client-side. Replaced with valid numbers in a block distinct from
-- anything a developer is likely to type manually while exercising signup by hand.

UPDATE identity.users
SET password_hash = '$2a$12$CbYG5ktQtoAsYdceLJtqCumeCkjw/WNjCds6ZzRab4An2qGbEvIFC',
    auth_status = 'ACTIVE',
    phone_number = CASE id
        WHEN '99000000-0000-0000-0000-000000000001' THEN '+917000000001'
        WHEN '99000000-0000-0000-0000-000000000002' THEN '+917000000002'
        WHEN '99000000-0000-0000-0000-000000000003' THEN '+917000000003'
        ELSE phone_number
    END,
    created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000001'),
    updated_by = COALESCE(updated_by, '00000000-0000-0000-0000-000000000001')
WHERE id IN (
    '99000000-0000-0000-0000-000000000001',
    '99000000-0000-0000-0000-000000000002',
    '99000000-0000-0000-0000-000000000003'
);

UPDATE community.groups
SET created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000001'),
    updated_by = COALESCE(updated_by, '00000000-0000-0000-0000-000000000001')
WHERE id = '99000001-0000-0000-0000-000000000001';

UPDATE community.group_members
SET created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000001'),
    updated_by = COALESCE(updated_by, '00000000-0000-0000-0000-000000000001')
WHERE id IN (
    '99000002-0000-0000-0000-000000000001',
    '99000002-0000-0000-0000-000000000002',
    '99000002-0000-0000-0000-000000000003'
);

UPDATE community.monthly_cycles
SET created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000001'),
    updated_by = COALESCE(updated_by, '00000000-0000-0000-0000-000000000001')
WHERE id = '99000003-0000-0000-0000-000000000001';

UPDATE finance.payments
SET created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000001'),
    updated_by = COALESCE(updated_by, '00000000-0000-0000-0000-000000000001')
WHERE id = '99000004-0000-0000-0000-000000000001';

UPDATE finance.receipts
SET created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000001'),
    updated_by = COALESCE(updated_by, '00000000-0000-0000-0000-000000000001')
WHERE id = '99000005-0000-0000-0000-000000000001';

UPDATE community.draws
SET created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000001'),
    updated_by = COALESCE(updated_by, '00000000-0000-0000-0000-000000000001')
WHERE id = '99000006-0000-0000-0000-000000000001';
