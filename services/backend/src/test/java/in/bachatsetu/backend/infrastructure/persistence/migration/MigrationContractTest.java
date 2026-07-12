package in.bachatsetu.backend.infrastructure.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationContractTest {

    private static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");
    private static final Comparator<String> BY_MIGRATION_VERSION = Comparator.comparingInt(
            fileName -> Integer.parseInt(fileName.substring(1, fileName.indexOf("__"))));

    @Test
    void containsOnlyTheEighteenOrderedVersionedMigrations() throws IOException {
        try (var files = Files.list(MIGRATION_DIRECTORY)) {
            assertThat(files.map(path -> path.getFileName().toString()).sorted(BY_MIGRATION_VERSION).toList())
                    .containsExactly(
                            "V1__initial_schema.sql",
                            "V2__seed_roles_permissions.sql",
                            "V3__identity_persistence.sql",
                            "V4__secure_otp_authentication.sql",
                            "V5__refresh_token_security.sql",
                            "V6__savings_group_schema.sql",
                            "V7__payment_gateway_orders.sql",
                            "V8__storage_files.sql",
                            "V9__audit_module.sql",
                            "V10__admin_analytics_audit_event.sql",
                            "V11__platform_configuration.sql",
                            "V12__signup_and_profile_onboarding.sql",
                            "V13__group_invitations.sql",
                            "V14__support_and_platform_operations.sql",
                            "V15__otp_send_failed_audit_event.sql",
                            "V16__backfill_role_permission_audit_metadata.sql",
                            "V17__email_audit_event.sql",
                            "V18__login_failed_token_refresh_audit_event.sql");
        }
    }

    @Test
    void initialSchemaCreatesEveryMappedEntityTable() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V1__initial_schema.sql"));
        List<String> tables = List.of(
                "identity.users",
                "identity.roles",
                "identity.permissions",
                "community.groups",
                "community.group_members",
                "community.monthly_cycles",
                "community.installments",
                "community.draws",
                "community.auction_bids",
                "finance.payments",
                "finance.receipts",
                "notification.notifications",
                "audit.audit_logs");

        assertThat(sql).containsSubsequence(tables.stream()
                .map(table -> "CREATE TABLE " + table)
                .toArray(String[]::new));
        assertThat(count(sql, "CREATE TABLE ")).isEqualTo(13);
        assertThat(sql)
                .contains("PRIMARY KEY")
                .contains("FOREIGN KEY")
                .contains("UNIQUE")
                .contains("CHECK")
                .contains("CREATE INDEX")
                .contains("version BIGINT NOT NULL")
                .contains("is_deleted BOOLEAN NOT NULL");
    }

    @Test
    void migrationsContainNoDestructiveOrNonTransactionalStatements() throws IOException {
        String migrations = Files.readString(MIGRATION_DIRECTORY.resolve("V1__initial_schema.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V2__seed_roles_permissions.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V3__identity_persistence.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V4__secure_otp_authentication.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V5__refresh_token_security.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V6__savings_group_schema.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V7__payment_gateway_orders.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V8__storage_files.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V9__audit_module.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V10__admin_analytics_audit_event.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V11__platform_configuration.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V12__signup_and_profile_onboarding.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V13__group_invitations.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V14__support_and_platform_operations.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V15__otp_send_failed_audit_event.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V16__backfill_role_permission_audit_metadata.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V17__email_audit_event.sql"))
                + Files.readString(MIGRATION_DIRECTORY.resolve("V18__login_failed_token_refresh_audit_event.sql"));
        String upperCaseSql = migrations.toUpperCase();

        assertThat(upperCaseSql)
                .doesNotContain("DROP TABLE")
                .doesNotContain("DROP SCHEMA")
                .doesNotContain("TRUNCATE ")
                .doesNotContain("CREATE INDEX CONCURRENTLY")
                .doesNotContain("COMMIT;");
    }

    @Test
    void seedMigrationUsesStableIdentifiersAndConflictSafeWrites() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V2__seed_roles_permissions.sql"));

        assertThat(sql)
                .contains("PLATFORM_ADMIN")
                .contains("GROUP_ORGANIZER")
                .contains("GROUP_MEMBER")
                .contains("PAYMENT.RECONCILE")
                .contains("AUDIT.READ")
                .contains("ON CONFLICT (id) DO UPDATE")
                .doesNotContain("gen_random_uuid()")
                .doesNotContain("CURRENT_TIMESTAMP");
    }

    @Test
    void identityMigrationExtendsCanonicalTablesWithoutDuplicates() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V3__identity_persistence.sql"));

        assertThat(sql)
                .contains("ALTER TABLE identity.users")
                .contains("CREATE TABLE identity.user_roles")
                .contains("CREATE TABLE identity.role_permissions")
                .contains("CREATE TABLE identity.refresh_tokens")
                .contains("CREATE TABLE identity.otp_verifications")
                .doesNotContain("CREATE TABLE identity.users")
                .doesNotContain("CREATE TABLE identity.roles")
                .doesNotContain("CREATE TABLE identity.permissions");
    }

    @Test
    void secureOtpMigrationRemovesPlaintextAndEnforcesPolicyLimits() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V4__secure_otp_authentication.sql"));

        assertThat(sql)
                .contains("SET otp_hash = crypt(otp_code, gen_salt('bf', 12))")
                .contains("DROP COLUMN otp_code")
                .contains("verification_attempts BETWEEN 0 AND 5")
                .contains("resend_count BETWEEN 0 AND 3")
                .contains("CREATE UNIQUE INDEX uk_otp_active_user_purpose")
                .contains("'INVALIDATED'");
    }

    @Test
    void refreshTokenSecurityMigrationHashesAndScopesCredentials() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V5__refresh_token_security.sql"));

        assertThat(sql)
                .contains("ADD COLUMN token_hash VARCHAR(255)")
                .contains("ADD COLUMN session_id UUID")
                .contains("ADD COLUMN tenant_id UUID")
                .contains("'ROTATED', 'REUSED'")
                .contains("CREATE UNIQUE INDEX uk_refresh_tokens_active_session")
                .contains("fk_refresh_tokens_replacement")
                .doesNotContain("token_value")
                .doesNotContain("plain_token");
    }

    @Test
    void savingsGroupMigrationEvolvesCanonicalTablesAndAddsHistory() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V6__savings_group_schema.sql"));

        assertThat(sql)
                .contains("ALTER TABLE community.groups")
                .contains("ADD COLUMN description TEXT")
                .contains("CREATE TABLE community.membership_history")
                .contains("status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'SUSPENDED')")
                .contains("maximum_members BETWEEN minimum_members AND 500")
                .contains("CREATE UNIQUE INDEX uk_groups_tenant_code")
                .contains("WHERE is_deleted = FALSE")
                .contains("ON DELETE RESTRICT")
                .contains("version BIGINT NOT NULL DEFAULT 0")
                .contains("ON CONFLICT (group_member_id, event_type) DO NOTHING")
                .doesNotContain("CREATE TABLE community.groups")
                .doesNotContain("CREATE TABLE community.group_members");
    }

    @Test
    void paymentGatewayMigrationAddsAnAdditiveOrdersTableOnly() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V7__payment_gateway_orders.sql"));

        assertThat(sql)
                .contains("CREATE TABLE finance.payment_gateway_orders")
                .contains("REFERENCES finance.payments (id)")
                .contains("uk_gateway_orders_payment")
                .contains("uk_gateway_orders_provider_order")
                .contains("gateway_type IN ('RAZORPAY', 'STRIPE', 'CASHFREE')")
                .contains("version BIGINT NOT NULL DEFAULT 0")
                .contains("is_deleted BOOLEAN NOT NULL DEFAULT FALSE")
                .doesNotContain("ALTER TABLE finance.payments")
                .doesNotContain("DROP ");
    }

    @Test
    void storageMigrationAddsAnAdditiveStoredFilesTableOnly() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V8__storage_files.sql"));

        assertThat(sql)
                .contains("CREATE SCHEMA IF NOT EXISTS storage")
                .contains("CREATE TABLE storage.stored_files")
                .contains("provider IN ('LOCAL', 'AWS_S3', 'AZURE_BLOB', 'GOOGLE_CLOUD_STORAGE')")
                .contains("version BIGINT NOT NULL DEFAULT 0")
                .contains("is_deleted BOOLEAN NOT NULL DEFAULT FALSE")
                .doesNotContain("ALTER TABLE")
                .doesNotContain("DROP ");
    }

    @Test
    void auditMigrationAddsAnAdditiveAuditEntriesTableOnly() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V9__audit_module.sql"));

        assertThat(sql)
                .contains("CREATE TABLE audit.audit_entries")
                .contains("event_type IN (")
                .contains("'LOGIN'")
                .contains("'SYSTEM_EVENT'")
                .contains("version BIGINT NOT NULL DEFAULT 0")
                .contains("is_deleted BOOLEAN NOT NULL DEFAULT FALSE")
                .doesNotContain("ALTER TABLE")
                .doesNotContain("CREATE SCHEMA")
                .doesNotContain("DROP ");
    }

    @Test
    void adminAnalyticsMigrationOnlyWidensTheAuditEventTypeConstraint() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V10__admin_analytics_audit_event.sql"));

        assertThat(sql)
                .contains("DROP CONSTRAINT ck_audit_entries_event_type")
                .contains("ADD CONSTRAINT ck_audit_entries_event_type CHECK (event_type IN (")
                .contains("'ADMIN_ANALYTICS_VIEWED'")
                .contains("'LOGIN'")
                .contains("'SYSTEM_EVENT'")
                .doesNotContain("CREATE TABLE")
                .doesNotContain("CREATE SCHEMA")
                .doesNotContain("DROP TABLE");
    }

    @Test
    void platformConfigurationMigrationCreatesAnAdditiveSchemaAndSeedsDefaults() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V11__platform_configuration.sql"));

        assertThat(sql)
                .contains("CREATE SCHEMA IF NOT EXISTS config")
                .contains("CREATE TABLE config.platform_configuration")
                .contains("CREATE TABLE config.feature_flags")
                .contains("CREATE TABLE config.platform_limits")
                .contains("ck_platform_configuration_singleton CHECK (id = 1)")
                .contains("'AUTHENTICATION'")
                .contains("'SIGNUP'")
                .contains("'MAX_GROUPS'")
                .contains("ON CONFLICT (id) DO NOTHING")
                .contains("ON CONFLICT (feature_key) DO NOTHING")
                .contains("ON CONFLICT (limit_key) DO NOTHING")
                .contains("DROP CONSTRAINT ck_audit_entries_event_type")
                .contains("'PLATFORM_CONFIGURATION_UPDATED'")
                .contains("'FEATURE_FLAG_UPDATED'")
                .contains("'SYSTEM_LIMIT_UPDATED'")
                .doesNotContain("ALTER TABLE community")
                .doesNotContain("ALTER TABLE finance")
                .doesNotContain("DROP TABLE");
    }

    @Test
    void signupAndProfileOnboardingMigrationOnlyAddsColumnsAndWidensTheAuditConstraint() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V12__signup_and_profile_onboarding.sql"));

        assertThat(sql)
                .contains("ALTER TABLE identity.users")
                .contains("ADD COLUMN city VARCHAR(100)")
                .contains("ADD COLUMN state VARCHAR(100)")
                .contains("ADD COLUMN photo_file_id UUID")
                .contains("ADD COLUMN notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE")
                .contains("ADD COLUMN onboarded BOOLEAN NOT NULL DEFAULT FALSE")
                .contains("DROP CONSTRAINT ck_audit_entries_event_type")
                .contains("'USER_REGISTERED'")
                .contains("'PROFILE_COMPLETED'")
                .contains("'INVITATION_CREATED'")
                .contains("'INVITATION_REVOKED'")
                .contains("'GROUP_JOINED'")
                .contains("'QR_JOINED'")
                .contains("'LINK_JOINED'")
                .doesNotContain("CREATE TABLE")
                .doesNotContain("DROP TABLE");
    }

    @Test
    void groupInvitationsMigrationAddsAnAdditiveTableOnly() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V13__group_invitations.sql"));

        assertThat(sql)
                .contains("CREATE TABLE community.group_invitations")
                .contains("invitation_type IN ('QR', 'CODE', 'LINK')")
                .contains("status IN ('ACTIVE', 'USED', 'EXPIRED', 'CANCELLED')")
                .contains("uk_group_invitations_active_per_group")
                .contains("version BIGINT NOT NULL DEFAULT 0")
                .contains("is_deleted BOOLEAN NOT NULL DEFAULT FALSE")
                .doesNotContain("ALTER TABLE")
                .doesNotContain("CREATE SCHEMA")
                .doesNotContain("DROP ");
    }

    @Test
    void supportAndPlatformOperationsMigrationAddsAdditiveSchemasOnly() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V14__support_and_platform_operations.sql"));

        assertThat(sql)
                .contains("CREATE SCHEMA IF NOT EXISTS support")
                .contains("CREATE TABLE support.support_tickets")
                .contains("category IN ('LOGIN', 'OTP', 'PAYMENT', 'GROUP', 'DRAW', 'NOTIFICATION', 'RECEIPT', 'STORAGE', 'OTHER')")
                .contains("status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')")
                .contains("CREATE SCHEMA IF NOT EXISTS platform")
                .contains("CREATE TABLE platform.tenants")
                .contains("CREATE TABLE platform.announcements")
                .contains("ck_tenants_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))")
                .contains("DROP CONSTRAINT ck_audit_entries_event_type")
                .contains("'TENANT_SUSPENDED'")
                .contains("'SUPPORT_TICKET_CREATED'")
                .contains("'ANNOUNCEMENT_PUBLISHED'")
                .contains("'BROADCAST_NOTIFICATION_SENT'")
                .doesNotContain("DROP TABLE")
                .doesNotContain("DROP SCHEMA");
    }

    @Test
    void otpSendFailedMigrationOnlyWidensTheAuditEventTypeConstraint() throws IOException {
        String sql = Files.readString(MIGRATION_DIRECTORY.resolve("V15__otp_send_failed_audit_event.sql"));

        assertThat(sql)
                .contains("DROP CONSTRAINT ck_audit_entries_event_type")
                .contains("ADD CONSTRAINT ck_audit_entries_event_type CHECK (event_type IN (")
                .contains("'OTP_SEND_FAILED'")
                .contains("'LOGIN'")
                .contains("'SYSTEM_EVENT'")
                .doesNotContain("CREATE TABLE")
                .doesNotContain("CREATE SCHEMA")
                .doesNotContain("DROP TABLE");
    }

    private int count(String source, String token) {
        return (source.length() - source.replace(token, "").length()) / token.length();
    }
}
