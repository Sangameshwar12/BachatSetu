package in.bachatsetu.backend.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.query.PlatformStatisticsResult;
import in.bachatsetu.backend.admin.domain.model.PlatformStatistics;
import in.bachatsetu.backend.admin.domain.port.PlatformStatisticsRepository;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetPlatformStatisticsApplicationServiceTest {

    private PlatformStatisticsRepository repository;
    private GetPlatformStatisticsApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(PlatformStatisticsRepository.class);
        service = new GetPlatformStatisticsApplicationService(
                repository, new DirectTransactionPort(), new AdminApplicationMapper());
    }

    @Test
    void computesAndMapsStatistics() {
        when(repository.compute()).thenReturn(new PlatformStatistics(10, 8, 2, 5, 4, 20, 15, 15, 30, 7));

        PlatformStatisticsResult result = service.execute();

        assertThat(result.totalUsers()).isEqualTo(10);
        assertThat(result.activeUsers()).isEqualTo(8);
        assertThat(result.completedPayments()).isEqualTo(15);
    }

    @Test
    void rejectsNullConstructorDependencies() {
        AdminApplicationMapper mapper = new AdminApplicationMapper();
        TransactionPort transaction = new DirectTransactionPort();
        assertThatThrownBy(() -> new GetPlatformStatisticsApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetPlatformStatisticsApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetPlatformStatisticsApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
