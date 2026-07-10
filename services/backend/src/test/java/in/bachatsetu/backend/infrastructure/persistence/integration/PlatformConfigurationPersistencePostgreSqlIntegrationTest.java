package in.bachatsetu.backend.infrastructure.persistence.integration;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureFlag;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformLimit;
import in.bachatsetu.backend.admin.domain.configuration.port.FeatureFlagRepository;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformConfigurationRepository;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformLimitRepository;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.shared.domain.AggregateId;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
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
@Import(PlatformConfigurationPersistencePostgreSqlIntegrationTest.PlatformConfigPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class PlatformConfigurationPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f9200000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Autowired
    private PlatformConfigurationRepository configurationRepository;

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @Autowired
    private PlatformLimitRepository limitRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void readsTheSeededSingletonConfigurationRow() {
        PlatformConfiguration configuration = configurationRepository.find();

        assertThat(configuration.defaultLanguage()).isEqualTo("ENGLISH");
        assertThat(configuration.otpExpirySeconds()).isEqualTo(300);
        assertThat(configuration.maintenanceEnabled()).isFalse();
    }

    @Test
    @Transactional
    void updatesTheConfigurationAndPersistsTheChange() {
        PlatformConfiguration configuration = configurationRepository.find();
        AggregateId administratorId = AggregateId.newId();

        configuration.update(
                "HINDI", 600, "AWS_S3", "STRIPE", 5, 20_000_000L, 200, 40, true, "scheduled maintenance", NOW,
                NOW.plusSeconds(3600), administratorId, NOW);
        configurationRepository.save(configuration);
        flushAndClear();

        PlatformConfiguration reloaded = configurationRepository.find();
        assertThat(reloaded.defaultLanguage()).isEqualTo("HINDI");
        assertThat(reloaded.maintenanceEnabled()).isTrue();
        assertThat(reloaded.updatedBy()).isEqualTo(administratorId);
    }

    @Test
    @Transactional
    void readsAllNineSeededFeatureFlagsEnabledByDefault() {
        List<FeatureFlag> flags = featureFlagRepository.findAll();

        assertThat(flags).hasSize(9);
        assertThat(flags).allSatisfy(flag -> assertThat(flag.enabled()).isTrue());
    }

    @Test
    @Transactional
    void disablesAFeatureFlagAndPersistsTheChange() {
        FeatureFlag flag = featureFlagRepository.findByKey(FeatureKey.PAYMENTS).orElseThrow();
        AggregateId administratorId = AggregateId.newId();

        featureFlagRepository.save(flag.withEnabled(false, administratorId, NOW));
        flushAndClear();

        Optional<FeatureFlag> reloaded = featureFlagRepository.findByKey(FeatureKey.PAYMENTS);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().enabled()).isFalse();
    }

    @Test
    @Transactional
    void readsAllFiveSeededSystemLimits() {
        List<PlatformLimit> limits = limitRepository.findAll();

        assertThat(limits).hasSize(5);
        assertThat(limits).anySatisfy(limit -> assertThat(limit.key()).isEqualTo(LimitKey.MAX_GROUPS));
    }

    @Test
    @Transactional
    void updatesASystemLimitAndPersistsTheChange() {
        PlatformLimit limit = limitRepository.findByKey(LimitKey.MAX_GROUPS).orElseThrow();
        AggregateId administratorId = AggregateId.newId();

        limitRepository.save(limit.withValue(999, administratorId, NOW));
        flushAndClear();

        PlatformLimit reloaded = limitRepository.findByKey(LimitKey.MAX_GROUPS).orElseThrow();
        assertThat(reloaded.value()).isEqualTo(999);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PlatformConfigPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
