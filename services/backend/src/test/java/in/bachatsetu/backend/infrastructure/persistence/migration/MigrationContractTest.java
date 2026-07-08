package in.bachatsetu.backend.infrastructure.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationContractTest {

    private static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");

    @Test
    void containsOnlyTheSevenOrderedVersionedMigrations() throws IOException {
        try (var files = Files.list(MIGRATION_DIRECTORY)) {
            assertThat(files.map(path -> path.getFileName().toString()).sorted().toList())
                    .containsExactly(
                            "V1__initial_schema.sql",
                            "V2__seed_roles_permissions.sql",
                            "V3__identity_persistence.sql",
                            "V4__secure_otp_authentication.sql",
                            "V5__refresh_token_security.sql",
                            "V6__savings_group_schema.sql",
                            "V7__payment_gateway_orders.sql");
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
                + Files.readString(MIGRATION_DIRECTORY.resolve("V7__payment_gateway_orders.sql"));
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

    private int count(String source, String token) {
        return (source.length() - source.replace(token, "").length()) / token.length();
    }
}
