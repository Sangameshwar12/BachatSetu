package in.bachatsetu.backend.admin.application.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.configuration.mapper.PlatformConfigApplicationMapper;
import in.bachatsetu.backend.admin.application.configuration.query.FeatureFlagResult;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureFlag;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.admin.domain.configuration.port.FeatureFlagRepository;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GetFeatureFlagsApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Test
    void computesAndMapsAllFeatureFlags() {
        FeatureFlagRepository repository = mock(FeatureFlagRepository.class);
        when(repository.findAll()).thenReturn(List.of(FeatureFlag.defaultEnabled(FeatureKey.PAYMENTS, NOW)));
        GetFeatureFlagsApplicationService service = new GetFeatureFlagsApplicationService(
                repository, new DirectTransactionPort(), new PlatformConfigApplicationMapper());

        List<FeatureFlagResult> results = service.execute();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).key()).isEqualTo("PAYMENTS");
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
