package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

class KnownTenantsRepositoryAdapterTest {

    @Test
    void listsKnownTenantIdsFromDistinctUserTenantIds() {
        UserSpringDataRepository userRepository = mock(UserSpringDataRepository.class);
        UUID tenantId = UUID.randomUUID();
        when(userRepository.findDistinctTenantIds(any()))
                .thenReturn(new PageImpl<>(java.util.List.of(tenantId)));
        KnownTenantsRepositoryAdapter adapter = new KnownTenantsRepositoryAdapter(userRepository);

        Page<AggregateId> page = adapter.listKnownTenantIds(new PageQuery(0, 20));

        assertThat(page.content()).containsExactly(new AggregateId(tenantId));
        assertThat(page.totalElements()).isEqualTo(1);
    }
}
