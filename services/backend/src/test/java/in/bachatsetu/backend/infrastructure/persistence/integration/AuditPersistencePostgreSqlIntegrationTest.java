package in.bachatsetu.backend.infrastructure.persistence.integration;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.audit.domain.port.AuditRepository;
import in.bachatsetu.backend.audit.domain.port.AuditSearchCriteria;
import in.bachatsetu.backend.audit.domain.port.AuditSortField;
import in.bachatsetu.backend.audit.domain.port.SortDirection;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.shared.domain.AggregateId;
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
@Import(AuditPersistencePostgreSqlIntegrationTest.AuditPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AuditPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");
    private static final UUID AUDITOR_ID = UUID.fromString("f8000000-0000-0000-0000-000000000001");

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndRehydratesAnAuditEntryIncludingMetadata() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        AggregateId resourceId = AggregateId.newId();
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), tenantId, actorId, AuditEventType.PAYMENT_VERIFIED, "payment", "Payment",
                resourceId, "PAYMENT_VERIFIED", "payment verified", "127.0.0.1", "test-agent",
                "{\"amountPaise\":50000}", NOW);

        auditRepository.save(entry);
        flushAndClear();

        AuditEntry restored = auditRepository.findById(tenantId, entry.id()).orElseThrow();
        assertThat(restored.eventType()).isEqualTo(AuditEventType.PAYMENT_VERIFIED);
        assertThat(restored.moduleName()).isEqualTo("payment");
        assertThat(restored.resourceType()).isEqualTo("Payment");
        assertThat(restored.resourceId()).isEqualTo(resourceId);
        assertThat(restored.metadata()).isEqualTo("{\"amountPaise\":50000}");
        assertThat(restored.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(restored.userAgent()).isEqualTo("test-agent");
        assertThat(restored.createdAt()).isEqualTo(NOW);
    }

    @Test
    @Transactional
    void persistsATenantLessAndActorLessEntry() {
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), null, null, AuditEventType.SYSTEM_EVENT, "automation", null, null,
                "SYSTEM_EVENT", null, null, null, null, NOW);

        auditRepository.save(entry);
        flushAndClear();

        AuditEntry restored = auditRepository.findById(null, entry.id()).orElseThrow();
        assertThat(restored.tenantId()).isNull();
        assertThat(restored.actorId()).isNull();
    }

    @Test
    @Transactional
    void anotherTenantCannotReadAnExistingEntry() {
        AggregateId tenantId = AggregateId.newId();
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), tenantId, AggregateId.newId(), AuditEventType.LOGIN, "auth", null, null,
                "LOGIN", null, null, null, null, NOW);
        auditRepository.save(entry);
        flushAndClear();

        assertThat(auditRepository.findById(AggregateId.newId(), entry.id())).isEmpty();
    }

    @Test
    @Transactional
    void searchFiltersByModuleAndEventTypeWithinOneTenant() {
        AggregateId tenantId = AggregateId.newId();
        save(tenantId, AuditEventType.LOGIN, "auth", NOW);
        save(tenantId, AuditEventType.PAYMENT_VERIFIED, "payment", NOW.plusSeconds(60));
        save(tenantId, AuditEventType.PAYMENT_VERIFIED, "payment", NOW.plusSeconds(120));
        flushAndClear();

        AuditPage<AuditEntry> result = auditRepository.search(new AuditSearchCriteria(
                tenantId, null, "payment", AuditEventType.PAYMENT_VERIFIED, null, null, 0, 20,
                AuditSortField.CREATED_AT, SortDirection.DESC));

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).allSatisfy(entry -> {
            assertThat(entry.moduleName()).isEqualTo("payment");
            assertThat(entry.eventType()).isEqualTo(AuditEventType.PAYMENT_VERIFIED);
        });
    }

    @Test
    @Transactional
    void searchFiltersByDateRange() {
        AggregateId tenantId = AggregateId.newId();
        save(tenantId, AuditEventType.LOGIN, "auth", NOW);
        save(tenantId, AuditEventType.LOGIN, "auth", NOW.plusSeconds(3600));
        save(tenantId, AuditEventType.LOGIN, "auth", NOW.plusSeconds(7200));
        flushAndClear();

        AuditPage<AuditEntry> result = auditRepository.search(new AuditSearchCriteria(
                tenantId, null, null, null, NOW.plusSeconds(1), NOW.plusSeconds(3700), 0, 20,
                AuditSortField.CREATED_AT, SortDirection.DESC));

        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @Transactional
    void searchPaginatesResults() {
        AggregateId tenantId = AggregateId.newId();
        for (int i = 0; i < 5; i++) {
            save(tenantId, AuditEventType.LOGIN, "auth", NOW.plusSeconds(i * 60L));
        }
        flushAndClear();

        AuditPage<AuditEntry> firstPage = auditRepository.search(new AuditSearchCriteria(
                tenantId, null, null, null, null, null, 0, 2, AuditSortField.CREATED_AT, SortDirection.DESC));
        AuditPage<AuditEntry> secondPage = auditRepository.search(new AuditSearchCriteria(
                tenantId, null, null, null, null, null, 1, 2, AuditSortField.CREATED_AT, SortDirection.DESC));

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(5);
        assertThat(firstPage.totalPages()).isEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.content()).hasSize(2);
        assertThat(secondPage.hasPrevious()).isTrue();
    }

    @Test
    @Transactional
    void searchSortsByCreatedAtInBothDirections() {
        AggregateId tenantId = AggregateId.newId();
        AuditEntry earlier = save(tenantId, AuditEventType.LOGIN, "auth", NOW);
        AuditEntry later = save(tenantId, AuditEventType.LOGIN, "auth", NOW.plusSeconds(60));
        flushAndClear();

        AuditPage<AuditEntry> descending = auditRepository.search(new AuditSearchCriteria(
                tenantId, null, null, null, null, null, 0, 20, AuditSortField.CREATED_AT, SortDirection.DESC));
        AuditPage<AuditEntry> ascending = auditRepository.search(new AuditSearchCriteria(
                tenantId, null, null, null, null, null, 0, 20, AuditSortField.CREATED_AT, SortDirection.ASC));

        assertThat(descending.content().get(0).id()).isEqualTo(later.id());
        assertThat(ascending.content().get(0).id()).isEqualTo(earlier.id());
    }

    @Test
    @Transactional
    void searchNeverLeaksAnotherTenantsEntries() {
        AggregateId tenantId = AggregateId.newId();
        save(tenantId, AuditEventType.LOGIN, "auth", NOW);
        save(AggregateId.newId(), AuditEventType.LOGIN, "auth", NOW);
        flushAndClear();

        AuditPage<AuditEntry> result = auditRepository.search(new AuditSearchCriteria(
                tenantId, null, null, null, null, null, 0, 20, AuditSortField.CREATED_AT, SortDirection.DESC));

        assertThat(result.totalElements()).isEqualTo(1);
    }

    private AuditEntry save(AggregateId tenantId, AuditEventType eventType, String moduleName, Instant createdAt) {
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), tenantId, AggregateId.newId(), eventType, moduleName, null, null,
                eventType.name(), null, null, null, null, createdAt);
        auditRepository.save(entry);
        return entry;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AuditPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
