package in.bachatsetu.backend.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformUserSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformUserRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSortField;
import in.bachatsetu.backend.admin.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListPlatformUsersApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private PlatformUserRepository repository;
    private ListPlatformUsersApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(PlatformUserRepository.class);
        service = new ListPlatformUsersApplicationService(
                repository, new DirectTransactionPort(), new AdminApplicationMapper());
    }

    @Test
    void searchesAndMapsAPageOfUsers() {
        PlatformUserSearchCriteria criteria = new PlatformUserSearchCriteria(
                PlatformUserStatus.ACTIVE, null, null, null, null, 0, 20, PlatformUserSortField.CREATED_AT,
                SortDirection.DESC);
        PlatformUserSummary summary = new PlatformUserSummary(
                AggregateId.newId(), AggregateId.newId(), null, null, null, null, PlatformUserStatus.ACTIVE, NOW);
        when(repository.search(criteria)).thenReturn(new PlatformPage<>(List.of(summary), 0, 20, 1));

        PlatformPage<PlatformUserResult> result = service.execute(criteria);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void returnsAnEmptyPageWhenNothingMatches() {
        PlatformUserSearchCriteria criteria = new PlatformUserSearchCriteria(
                null, null, null, null, null, 0, 20, PlatformUserSortField.CREATED_AT, SortDirection.DESC);
        when(repository.search(criteria)).thenReturn(new PlatformPage<>(List.of(), 0, 20, 0));

        assertThat(service.execute(criteria).content()).isEmpty();
    }

    @Test
    void rejectsANullCriteria() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullConstructorDependencies() {
        AdminApplicationMapper mapper = new AdminApplicationMapper();
        TransactionPort transaction = new DirectTransactionPort();
        assertThatThrownBy(() -> new ListPlatformUsersApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPlatformUsersApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPlatformUsersApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
