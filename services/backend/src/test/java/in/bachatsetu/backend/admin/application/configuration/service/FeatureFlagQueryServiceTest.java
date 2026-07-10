package in.bachatsetu.backend.admin.application.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.configuration.model.FeatureFlag;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.admin.domain.configuration.port.FeatureFlagRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FeatureFlagQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void reflectsAStoredDisabledFlag() {
        FeatureFlagRepository repository = mock(FeatureFlagRepository.class);
        when(repository.findByKey(FeatureKey.PAYMENTS)).thenReturn(Optional.of(
                FeatureFlag.defaultEnabled(FeatureKey.PAYMENTS, NOW).withEnabled(false, AggregateId.newId(), NOW)));
        FeatureFlagQueryService service = new FeatureFlagQueryService(repository);

        assertThat(service.isEnabled(FeatureKey.PAYMENTS)).isFalse();
    }

    @Test
    void failsOpenWhenNoRowExists() {
        FeatureFlagRepository repository = mock(FeatureFlagRepository.class);
        when(repository.findByKey(FeatureKey.AUCTION)).thenReturn(Optional.empty());
        FeatureFlagQueryService service = new FeatureFlagQueryService(repository);

        assertThat(service.isEnabled(FeatureKey.AUCTION)).isTrue();
    }
}
