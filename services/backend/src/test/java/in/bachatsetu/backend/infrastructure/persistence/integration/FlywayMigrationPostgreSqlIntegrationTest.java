package in.bachatsetu.backend.infrastructure.persistence.integration;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        classes = BachatSetuBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.clean-disabled=false",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.data.redis.repositories.enabled=false",
            "bachatsetu.persistence.auditing.enabled=true",
            "bachatsetu.persistence.repositories.enabled=true"
        })
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationPostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migratesCleanPostgreSqlAndMatchesHibernateMappings() {
        flyway.clean();

        var firstMigration = flyway.migrate();
        var secondMigration = flyway.migrate();

        assertThat(firstMigration.migrationsExecuted).isEqualTo(2);
        assertThat(secondMigration.migrationsExecuted).isZero();
        assertThat(countMappedTables()).isEqualTo(13);
        assertThat(count("identity.roles")).isEqualTo(5);
        assertThat(count("identity.permissions")).isEqualTo(17);
    }

    private int countMappedTables() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_type = 'BASE TABLE'
                  AND table_schema IN ('identity', 'community', 'finance', 'notification', 'audit')
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private int count(String qualifiedTableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + qualifiedTableName, Integer.class);
        return count == null ? 0 : count;
    }
}
