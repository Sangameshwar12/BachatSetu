package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.model.PlatformGroupStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.SortDirection;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class AdminGroupRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private SavingsGroupSpringDataRepository groupRepository;
    private MemberSpringDataRepository memberRepository;
    private AdminGroupRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        groupRepository = mock(SavingsGroupSpringDataRepository.class);
        memberRepository = mock(MemberSpringDataRepository.class);
        adapter = new AdminGroupRepositoryAdapter(groupRepository, memberRepository);
    }

    @Test
    void searchesAcrossTenantsAndMapsResultsIncludingMemberCount() {
        UUID groupId = UUID.randomUUID();
        SavingsGroupJpaEntity entity = mock(SavingsGroupJpaEntity.class);
        when(entity.getId()).thenReturn(groupId);
        when(entity.getTenantId()).thenReturn(UUID.randomUUID());
        when(entity.getCode()).thenReturn("GRP-1");
        when(entity.getName()).thenReturn("Neighborhood Bhishi");
        when(entity.getStatus()).thenReturn(GroupStatus.ACTIVE);
        when(entity.getCreatedAt()).thenReturn(NOW);
        Page<SavingsGroupJpaEntity> page = new PageImpl<>(List.of(entity));
        when(groupRepository.searchAcrossTenants(eq(GroupStatus.ACTIVE), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(memberRepository.countByGroup_IdAndDeletedFalse(groupId)).thenReturn(7L);
        PlatformGroupSearchCriteria criteria = new PlatformGroupSearchCriteria(
                PlatformGroupStatus.ACTIVE, null, null, 0, 20, SortDirection.DESC);

        PlatformPage<PlatformGroupSummary> result = adapter.search(criteria);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).memberCount()).isEqualTo(7);
        assertThat(result.content().get(0).code()).isEqualTo("GRP-1");
    }

    @Test
    void returnsAnEmptyPageWhenNothingMatches() {
        when(groupRepository.searchAcrossTenants(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        PlatformGroupSearchCriteria criteria = new PlatformGroupSearchCriteria(
                null, null, null, 0, 20, SortDirection.DESC);

        assertThat(adapter.search(criteria).content()).isEmpty();
    }
}
