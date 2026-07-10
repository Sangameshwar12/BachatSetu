package in.bachatsetu.backend.admin.application.configuration.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.admin.application.configuration.query.FeatureFlagResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureFlag;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;
import in.bachatsetu.backend.admin.domain.configuration.model.PlatformLimit;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformConfigApplicationMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");
    private final PlatformConfigApplicationMapper mapper = new PlatformConfigApplicationMapper();

    @Test
    void mapsPlatformConfiguration() {
        AggregateId actorId = AggregateId.newId();
        PlatformConfiguration configuration = PlatformConfiguration.of(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, true, "down", NOW, NOW.plusSeconds(1),
                2, NOW, actorId);

        PlatformConfigurationResult result = mapper.toResult(configuration);

        assertThat(result.defaultLanguage()).isEqualTo("ENGLISH");
        assertThat(result.maintenanceEnabled()).isTrue();
        assertThat(result.version()).isEqualTo(2);
        assertThat(result.updatedBy()).isEqualTo(actorId.value());
    }

    @Test
    void mapsAFeatureFlag() {
        FeatureFlag flag = FeatureFlag.defaultEnabled(FeatureKey.PAYMENTS, NOW);

        FeatureFlagResult result = mapper.toResult(flag);

        assertThat(result.key()).isEqualTo("PAYMENTS");
        assertThat(result.enabled()).isTrue();
        assertThat(result.updatedBy()).isNull();
    }

    @Test
    void mapsAPlatformLimit() {
        PlatformLimit limit = PlatformLimit.of(LimitKey.MAX_GROUPS, 500, NOW);

        PlatformLimitResult result = mapper.toResult(limit);

        assertThat(result.key()).isEqualTo("MAX_GROUPS");
        assertThat(result.value()).isEqualTo(500);
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> mapper.toResult((PlatformConfiguration) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResult((FeatureFlag) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResult((PlatformLimit) null)).isInstanceOf(NullPointerException.class);
    }
}
