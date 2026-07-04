package in.bachatsetu.backend.infrastructure.persistence.integration;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.infrastructure.persistence.audit.JpaAuditingConfig;
import in.bachatsetu.backend.infrastructure.persistence.config.PersistenceConfig;
import in.bachatsetu.backend.infrastructure.persistence.entity.PersistenceRecordStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.PermissionJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PermissionSpringDataRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
    "bachatsetu.persistence.auditing.enabled=true",
    "bachatsetu.persistence.repositories.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PersistenceConfig.class, JpaAuditingConfig.class})
@Testcontainers(disabledWithoutDocker = true)
class RepositoryLayerPostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private PermissionSpringDataRepository repository;

    @Test
    void persistsAndQueriesAgainstPostgreSql() {
        PermissionJpaEntity permission = new PermissionJpaEntity(
                UUID.randomUUID(),
                "GROUP.READ",
                "GROUP",
                "READ",
                "Read group details",
                PersistenceRecordStatus.ACTIVE);

        repository.saveAndFlush(permission);

        assertThat(repository.findByCodeAndDeletedFalse("GROUP.READ"))
                .contains(permission);
        assertThat(permission.getCreatedAt()).isNotNull();
    }
}
