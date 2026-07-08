package in.bachatsetu.backend.infrastructure.persistence.integration;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        classes = BachatSetuBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.flyway.enabled=true",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.data.redis.repositories.enabled=false",
            "bachatsetu.persistence.auditing.enabled=true",
            "bachatsetu.persistence.repositories.enabled=true"
        })
@Import(StoragePersistencePostgreSqlIntegrationTest.StoragePersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class StoragePersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");
    private static final UUID AUDITOR_ID = UUID.fromString("f7000000-0000-0000-0000-000000000001");

    @Autowired
    private StorageRepository storageRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndRehydratesAStoredFile() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        StoredFile file = StoredFile.upload(
                AggregateId.newId(), tenantId, StorageProvider.LOCAL, "/data/storage/tenant/file-1", "receipt.pdf",
                "application/pdf", 2048L, "checksum-1", actorId, NOW);

        storageRepository.save(file);
        flushAndClear();

        StoredFile restored = storageRepository.findById(tenantId, file.id()).orElseThrow();
        assertThat(restored.provider()).isEqualTo(StorageProvider.LOCAL);
        assertThat(restored.path()).isEqualTo("/data/storage/tenant/file-1");
        assertThat(restored.originalFilename()).isEqualTo("receipt.pdf");
        assertThat(restored.contentType()).isEqualTo("application/pdf");
        assertThat(restored.size()).isEqualTo(2048L);
        assertThat(restored.checksum()).isEqualTo("checksum-1");
        assertThat(restored.uploadedAt()).isEqualTo(NOW);
    }

    @Test
    @Transactional
    void softDeletedFilesAreNoLongerFound() {
        AggregateId tenantId = AggregateId.newId();
        StoredFile file = StoredFile.upload(
                AggregateId.newId(), tenantId, StorageProvider.LOCAL, "/data/storage/tenant/file-2", "file.txt",
                "text/plain", 10L, "checksum-2", AggregateId.newId(), NOW);
        storageRepository.save(file);
        flushAndClear();

        storageRepository.delete(tenantId, file.id());
        flushAndClear();

        assertThat(storageRepository.findById(tenantId, file.id())).isEmpty();
    }

    @Test
    @Transactional
    void reportsNoMatchForAnotherTenant() {
        AggregateId tenantId = AggregateId.newId();
        StoredFile file = StoredFile.upload(
                AggregateId.newId(), tenantId, StorageProvider.AWS_S3, "s3://bucket/key", "file.txt",
                "text/plain", 10L, "checksum-3", AggregateId.newId(), NOW);
        storageRepository.save(file);
        flushAndClear();

        assertThat(storageRepository.findById(AggregateId.newId(), file.id())).isEmpty();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StoragePersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
