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
    void containsOnlyTheTwoOrderedVersionedMigrations() throws IOException {
        try (var files = Files.list(MIGRATION_DIRECTORY)) {
            assertThat(files.map(path -> path.getFileName().toString()).sorted().toList())
                    .containsExactly("V1__initial_schema.sql", "V2__seed_roles_permissions.sql");
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
                + Files.readString(MIGRATION_DIRECTORY.resolve("V2__seed_roles_permissions.sql"));
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

    private int count(String source, String token) {
        return (source.length() - source.replace(token, "").length()) / token.length();
    }
}
