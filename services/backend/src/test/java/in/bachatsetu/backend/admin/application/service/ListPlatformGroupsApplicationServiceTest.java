package in.bachatsetu.backend.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.query.PlatformGroupResult;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListPlatformGroupsApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private PlatformGroupRepository repository;
    private ListPlatformGroupsApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(PlatformGroupRepository.class);
        service = new ListPlatformGroupsApplicationService(
                repository, new DirectTransactionPort(), new AdminApplicationMapper());
    }

    @Test
    void searchesAndMapsAPageOfGroups() {
        PlatformGroupSearchCriteria criteria = new PlatformGroupSearchCriteria(
                PlatformGroupStatus.ACTIVE, null, null, 0, 20, SortDirection.DESC);
        PlatformGroupSummary summary = new PlatformGroupSummary(
                AggregateId.newId(), AggregateId.newId(), "GRP-1", "name", PlatformGroupStatus.ACTIVE, 3, NOW);
        when(repository.search(criteria)).thenReturn(new PlatformPage<>(List.of(summary), 0, 20, 1));

        PlatformPage<PlatformGroupResult> result = service.execute(criteria);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).memberCount()).isEqualTo(3);
    }

    @Test
    void rejectsANullCriteria() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullConstructorDependencies() {
        AdminApplicationMapper mapper = new AdminApplicationMapper();
        TransactionPort transaction = new DirectTransactionPort();
        assertThatThrownBy(() -> new ListPlatformGroupsApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPlatformGroupsApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPlatformGroupsApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
