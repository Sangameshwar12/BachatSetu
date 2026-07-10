package in.bachatsetu.backend.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.query.PlatformTenantResult;
import in.bachatsetu.backend.admin.domain.model.PlatformTenantSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformPageRequest;
import in.bachatsetu.backend.admin.domain.port.PlatformTenantRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListPlatformTenantsApplicationServiceTest {

    private PlatformTenantRepository repository;
    private ListPlatformTenantsApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(PlatformTenantRepository.class);
        service = new ListPlatformTenantsApplicationService(
                repository, new DirectTransactionPort(), new AdminApplicationMapper());
    }

    @Test
    void listsAndMapsAPageOfTenants() {
        PlatformPageRequest pageRequest = new PlatformPageRequest(0, 20);
        PlatformTenantSummary summary = new PlatformTenantSummary(AggregateId.newId(), 5, 2);
        when(repository.search(pageRequest)).thenReturn(new PlatformPage<>(List.of(summary), 0, 20, 1));

        PlatformPage<PlatformTenantResult> result = service.execute(pageRequest);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).userCount()).isEqualTo(5);
    }

    @Test
    void rejectsANullPageRequest() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullConstructorDependencies() {
        AdminApplicationMapper mapper = new AdminApplicationMapper();
        TransactionPort transaction = new DirectTransactionPort();
        assertThatThrownBy(() -> new ListPlatformTenantsApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPlatformTenantsApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPlatformTenantsApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
