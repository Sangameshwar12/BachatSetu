package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.model.PlatformTenantSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformPageRequest;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class AdminTenantRepositoryAdapterTest {

    private UserSpringDataRepository userRepository;
    private SavingsGroupSpringDataRepository groupRepository;
    private AdminTenantRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserSpringDataRepository.class);
        groupRepository = mock(SavingsGroupSpringDataRepository.class);
        adapter = new AdminTenantRepositoryAdapter(userRepository, groupRepository);
    }

    @Test
    void listsDistinctTenantsWithPerTenantTotals() {
        UUID tenantId = UUID.randomUUID();
        Page<UUID> tenantIds = new PageImpl<>(List.of(tenantId));
        when(userRepository.findDistinctTenantIds(PageRequest.of(0, 20))).thenReturn(tenantIds);
        when(userRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(5L);
        when(groupRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(2L);

        PlatformPage<PlatformTenantSummary> result = adapter.search(new PlatformPageRequest(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).userCount()).isEqualTo(5);
        assertThat(result.content().get(0).groupCount()).isEqualTo(2);
    }

    @Test
    void returnsAnEmptyPageWhenThereAreNoTenants() {
        when(userRepository.findDistinctTenantIds(PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of()));

        assertThat(adapter.search(new PlatformPageRequest(0, 20)).content()).isEmpty();
    }
}
