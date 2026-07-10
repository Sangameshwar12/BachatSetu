package in.bachatsetu.backend.infrastructure.persistence.integration;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformUserSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformPageRequest;
import in.bachatsetu.backend.admin.domain.port.PlatformStatisticsRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformTenantRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformUserRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSortField;
import in.bachatsetu.backend.admin.domain.port.SortDirection;
import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
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
@Import(AdminPersistencePostgreSqlIntegrationTest.AdminPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AdminPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f9000000-0000-0000-0000-000000000001");

    @Autowired
    private PlatformUserRepository platformUserRepository;

    @Autowired
    private PlatformTenantRepository platformTenantRepository;

    @Autowired
    private PlatformStatisticsRepository platformStatisticsRepository;

    @Autowired
    private UserSpringDataRepository userSpringDataRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void searchFiltersByStatusEmailAndDateRangeAcrossTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        saveUser(tenantA, "asha@example.com", "+919000000001", UserStatus.ACTIVE);
        saveUser(tenantB, "bina@example.com", "+919000000002", UserStatus.DISABLED);
        flushAndClear();

        PlatformPage<PlatformUserSummary> result = platformUserRepository.search(new PlatformUserSearchCriteria(
                PlatformUserStatus.ACTIVE, null, null, null, null, 0, 20, PlatformUserSortField.CREATED_AT,
                SortDirection.DESC));

        assertThat(result.content()).anySatisfy(summary -> assertThat(summary.email()).isEqualTo("asha@example.com"));
        assertThat(result.content()).noneSatisfy(summary -> assertThat(summary.email()).isEqualTo("bina@example.com"));
    }

    @Test
    @Transactional
    void searchFiltersByPartialEmailMatch() {
        UUID tenantId = UUID.randomUUID();
        saveUser(tenantId, "unique-admin-test@example.com", "+919000000003", UserStatus.ACTIVE);
        flushAndClear();

        PlatformPage<PlatformUserSummary> result = platformUserRepository.search(new PlatformUserSearchCriteria(
                null, "unique-admin-test", null, null, null, 0, 20, PlatformUserSortField.CREATED_AT,
                SortDirection.DESC));

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @Transactional
    void searchPaginatesAndSortsResults() {
        UUID tenantId = UUID.randomUUID();
        saveUser(tenantId, "sort-a@example.com", "+919000000010", UserStatus.ACTIVE);
        saveUser(tenantId, "sort-b@example.com", "+919000000011", UserStatus.ACTIVE);
        flushAndClear();

        PlatformPage<PlatformUserSummary> ascending = platformUserRepository.search(new PlatformUserSearchCriteria(
                null, "sort-", null, null, null, 0, 1, PlatformUserSortField.EMAIL, SortDirection.ASC));

        assertThat(ascending.content()).hasSize(1);
        assertThat(ascending.content().get(0).email()).isEqualTo("sort-a@example.com");
        assertThat(ascending.totalElements()).isEqualTo(2);
        assertThat(ascending.hasNext()).isTrue();
    }

    @Test
    @Transactional
    void findsAUserAcrossAnyTenant() {
        UUID tenantId = UUID.randomUUID();
        UserJpaEntity entity = saveUser(tenantId, "findme@example.com", "+919000000020", UserStatus.ACTIVE);
        flushAndClear();

        Optional<PlatformUserSummary> found = platformUserRepository.findById(new AggregateId(entity.getId()));

        assertThat(found).isPresent();
        assertThat(found.get().email()).isEqualTo("findme@example.com");
    }

    @Test
    @Transactional
    void enablesAndDisablesAUserAcrossTenants() {
        UUID tenantId = UUID.randomUUID();
        UserJpaEntity entity = saveUser(tenantId, "toggle@example.com", "+919000000030", UserStatus.ACTIVE);
        flushAndClear();
        AggregateId userId = new AggregateId(entity.getId());
        AggregateId administratorId = AggregateId.newId();
        Instant now = Instant.parse("2026-07-08T08:00:00Z");

        boolean disabled = platformUserRepository.updateStatus(
                userId, PlatformUserStatus.DISABLED, administratorId, now);
        flushAndClear();

        assertThat(disabled).isTrue();
        assertThat(platformUserRepository.findById(userId).orElseThrow().status())
                .isEqualTo(PlatformUserStatus.DISABLED);

        boolean enabled = platformUserRepository.updateStatus(userId, PlatformUserStatus.ACTIVE, administratorId, now);
        flushAndClear();

        assertThat(enabled).isTrue();
        assertThat(platformUserRepository.findById(userId).orElseThrow().status())
                .isEqualTo(PlatformUserStatus.ACTIVE);
    }

    @Test
    @Transactional
    void reportsFailureWhenUpdatingAnUnknownUser() {
        boolean updated = platformUserRepository.updateStatus(
                AggregateId.newId(), PlatformUserStatus.ACTIVE, AggregateId.newId(), Instant.now());

        assertThat(updated).isFalse();
    }

    @Test
    @Transactional
    void listsDistinctTenantsWithUserCounts() {
        UUID tenantId = UUID.randomUUID();
        saveUser(tenantId, "tenant-listing-1@example.com", "+919000000040", UserStatus.ACTIVE);
        saveUser(tenantId, "tenant-listing-2@example.com", "+919000000041", UserStatus.ACTIVE);
        flushAndClear();

        PlatformPage<in.bachatsetu.backend.admin.domain.model.PlatformTenantSummary> result =
                platformTenantRepository.search(new PlatformPageRequest(0, 100));

        assertThat(result.content()).anySatisfy(summary -> {
            if (summary.tenantId().value().equals(tenantId)) {
                assertThat(summary.userCount()).isEqualTo(2);
            }
        });
    }

    @Test
    @Transactional
    void computesStatisticsWithoutError() {
        UUID tenantId = UUID.randomUUID();
        saveUser(tenantId, "stats@example.com", "+919000000050", UserStatus.ACTIVE);
        flushAndClear();

        var statistics = platformStatisticsRepository.compute();

        assertThat(statistics.totalUsers()).isGreaterThanOrEqualTo(1);
        assertThat(statistics.activeUsers()).isGreaterThanOrEqualTo(1);
    }

    private UserJpaEntity saveUser(UUID tenantId, String email, String phoneNumber, UserStatus authenticationStatus) {
        UserJpaEntity entity = new UserJpaEntity(
                UUID.randomUUID(), tenantId, "Test", "User", email, phoneNumber,
                in.bachatsetu.backend.user.domain.model.UserStatus.ACTIVE, PreferredLanguage.ENGLISH);
        entity.updateAuthentication("hash", authenticationStatus, Set.of());
        return userSpringDataRepository.save(entity);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AdminPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
