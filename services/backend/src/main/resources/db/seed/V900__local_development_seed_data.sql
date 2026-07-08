-- Local development / QA sample data.
--
-- Applied only when the "seed" Spring profile is active (see application-seed.yml), which adds
-- classpath:db/seed as an additional Flyway location alongside the real classpath:db/migration
-- schema history. It is never combined into the default "local" profile so it cannot affect the
-- FlywayMigrationPostgreSqlIntegrationTest's fixed "6 migrations" assertion or leak into dev/prod.
--
-- Tenant: the fixed placeholder UUID returned by LocalTenantScopeProviderConfig
-- (00000000-0000-0000-0000-000000000000) so seeded records are visible through the same
-- tenant-scoped queries the local-only auth adapters use.

INSERT INTO identity.users (
    id, tenant_id, given_name, family_name, email, phone_number, status, language_code,
    created_at, updated_at
) VALUES
    ('99000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000',
     'Asha', 'Patil', 'asha.organizer@example.com', '+911234567890', 'ACTIVE', 'ENGLISH',
     TIMESTAMPTZ '2026-01-01 00:00:00+00', TIMESTAMPTZ '2026-01-01 00:00:00+00'),
    ('99000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000000',
     'Ravi', 'Kulkarni', 'ravi.member1@example.com', '+911234567891', 'ACTIVE', 'ENGLISH',
     TIMESTAMPTZ '2026-01-01 00:00:00+00', TIMESTAMPTZ '2026-01-01 00:00:00+00'),
    ('99000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000000',
     'Sita', 'Joshi', 'sita.member2@example.com', '+911234567892', 'ACTIVE', 'ENGLISH',
     TIMESTAMPTZ '2026-01-01 00:00:00+00', TIMESTAMPTZ '2026-01-01 00:00:00+00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO community.groups (
    id, tenant_id, organizer_user_id, group_code, group_name, description, module_type, status,
    contribution_amount_paise, currency_code, frequency, start_date, duration_cycles,
    minimum_members, maximum_members, payout_method, partial_payment_allowed,
    created_at, updated_at
) VALUES (
    '99000001-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000',
    '99000000-0000-0000-0000-000000000001', 'BHISHI001', 'Diwali Bachat Bhishi',
    'Sample local development savings group seeded for QA.', 'BHISHI', 'ACTIVE',
    500000, 'INR', 'MONTHLY', DATE '2026-01-01', 12,
    2, 20, 'RANDOM_DRAW', FALSE,
    TIMESTAMPTZ '2026-01-01 00:00:00+00', TIMESTAMPTZ '2026-01-01 00:00:00+00'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO community.group_members (
    id, tenant_id, group_id, user_id, member_number, role_in_group, status, joined_at,
    created_at, updated_at
) VALUES
    ('99000002-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000',
     '99000001-0000-0000-0000-000000000001', '99000000-0000-0000-0000-000000000001',
     'M001', 'ORGANIZER', 'ACTIVE', TIMESTAMPTZ '2026-01-01 00:00:00+00',
     TIMESTAMPTZ '2026-01-01 00:00:00+00', TIMESTAMPTZ '2026-01-01 00:00:00+00'),
    ('99000002-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000000',
     '99000001-0000-0000-0000-000000000001', '99000000-0000-0000-0000-000000000002',
     'M002', 'MEMBER', 'ACTIVE', TIMESTAMPTZ '2026-01-01 00:00:00+00',
     TIMESTAMPTZ '2026-01-01 00:00:00+00', TIMESTAMPTZ '2026-01-01 00:00:00+00'),
    ('99000002-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000000',
     '99000001-0000-0000-0000-000000000001', '99000000-0000-0000-0000-000000000003',
     'M003', 'MEMBER', 'ACTIVE', TIMESTAMPTZ '2026-01-01 00:00:00+00',
     TIMESTAMPTZ '2026-01-01 00:00:00+00', TIMESTAMPTZ '2026-01-01 00:00:00+00')
ON CONFLICT (id) DO NOTHING;

-- A single closed monthly cycle so the seeded draw has a valid, non-nullable cycle_id to reference.
INSERT INTO community.monthly_cycles (
    id, tenant_id, group_id, cycle_number, cycle_month, due_date, status, opened_at, closed_at,
    created_at, updated_at
) VALUES (
    '99000003-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000',
    '99000001-0000-0000-0000-000000000001', 1, DATE '2026-01-01', DATE '2026-01-10', 'CLOSED',
    TIMESTAMPTZ '2026-01-01 00:00:00+00', TIMESTAMPTZ '2026-01-31 00:00:00+00',
    TIMESTAMPTZ '2026-01-01 00:00:00+00', TIMESTAMPTZ '2026-01-31 00:00:00+00'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO finance.payments (
    id, tenant_id, payer_user_id, group_id, payment_reference, amount_paise, currency_code,
    payment_method, status, idempotency_key_hash, reconciliation_status,
    created_at, updated_at
) VALUES (
    '99000004-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000',
    '99000000-0000-0000-0000-000000000002', '99000001-0000-0000-0000-000000000001',
    'PMT-SEED-0001', 500000, 'INR', 'UPI', 'VERIFIED', 'seed-idempotency-hash-0001', 'NOT_REQUIRED',
    TIMESTAMPTZ '2026-01-15 00:00:00+00', TIMESTAMPTZ '2026-01-15 00:00:00+00'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO finance.receipts (
    id, tenant_id, payment_id, user_id, receipt_number, receipt_date, amount_paise, currency_code,
    delivery_status, created_at, updated_at
) VALUES (
    '99000005-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000',
    '99000004-0000-0000-0000-000000000001', '99000000-0000-0000-0000-000000000002',
    'RCPT-SEED-0001', DATE '2026-01-15', 500000, 'INR', 'GENERATED',
    TIMESTAMPTZ '2026-01-15 00:00:00+00', TIMESTAMPTZ '2026-01-15 00:00:00+00'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO community.draws (
    id, tenant_id, group_id, cycle_id, selected_group_member_id, draw_type, status,
    scheduled_at, completed_at, payout_amount_paise, currency_code,
    created_at, updated_at
) VALUES (
    '99000006-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000',
    '99000001-0000-0000-0000-000000000001', '99000003-0000-0000-0000-000000000001',
    '99000002-0000-0000-0000-000000000002', 'RANDOM', 'COMPLETED',
    TIMESTAMPTZ '2026-01-20 00:00:00+00', TIMESTAMPTZ '2026-01-20 01:00:00+00', 1500000, 'INR',
    TIMESTAMPTZ '2026-01-20 00:00:00+00', TIMESTAMPTZ '2026-01-20 01:00:00+00'
) ON CONFLICT (id) DO NOTHING;
