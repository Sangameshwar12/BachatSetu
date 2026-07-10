package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.analytics.model.GroupAnalytics;
import in.bachatsetu.backend.draw.domain.model.DrawStatus;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.DrawSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminGroupAnalyticsRepositoryAdapterTest {

    @Test
    void computesGroupAnalyticsIncludingAveragesAndDrawCompletionRate() {
        SavingsGroupSpringDataRepository groupRepository = mock(SavingsGroupSpringDataRepository.class);
        MemberSpringDataRepository memberRepository = mock(MemberSpringDataRepository.class);
        DrawSpringDataRepository drawRepository = mock(DrawSpringDataRepository.class);
        when(groupRepository.countByDeletedFalse()).thenReturn(4L);
        when(groupRepository.countByStatusAndDeletedFalse(GroupStatus.ACTIVE)).thenReturn(3L);
        when(groupRepository.countByStatusAndDeletedFalse(GroupStatus.CLOSED)).thenReturn(1L);
        when(groupRepository.findAverageContributionAmountPaise()).thenReturn(250_000.0);
        when(groupRepository.findMonthlyNewGroupCounts())
                .thenReturn(List.<Object[]>of(new Object[] {2026, 7, 4L}));
        when(memberRepository.countByDeletedFalse()).thenReturn(20L);
        when(drawRepository.countByDeletedFalse()).thenReturn(8L);
        when(drawRepository.countByStatusAndDeletedFalse(DrawStatus.COMPLETED)).thenReturn(6L);
        AdminGroupAnalyticsRepositoryAdapter adapter =
                new AdminGroupAnalyticsRepositoryAdapter(groupRepository, memberRepository, drawRepository);

        GroupAnalytics analytics = adapter.compute();

        assertThat(analytics.totalGroups()).isEqualTo(4L);
        assertThat(analytics.averageMembersPerGroup()).isEqualTo(5.0);
        assertThat(analytics.averageContributionAmountPaise()).isEqualTo(250_000.0);
        assertThat(analytics.monthlyNewGroups()).hasSize(1);
        assertThat(analytics.drawCompletionRate()).isEqualTo(0.75);
    }

    @Test
    void returnsZeroAveragesWhenThereAreNoGroupsOrDraws() {
        SavingsGroupSpringDataRepository groupRepository = mock(SavingsGroupSpringDataRepository.class);
        MemberSpringDataRepository memberRepository = mock(MemberSpringDataRepository.class);
        DrawSpringDataRepository drawRepository = mock(DrawSpringDataRepository.class);
        when(groupRepository.countByDeletedFalse()).thenReturn(0L);
        when(groupRepository.findAverageContributionAmountPaise()).thenReturn(null);
        when(groupRepository.findMonthlyNewGroupCounts()).thenReturn(List.of());
        when(drawRepository.countByDeletedFalse()).thenReturn(0L);
        AdminGroupAnalyticsRepositoryAdapter adapter =
                new AdminGroupAnalyticsRepositoryAdapter(groupRepository, memberRepository, drawRepository);

        GroupAnalytics analytics = adapter.compute();

        assertThat(analytics.averageMembersPerGroup()).isZero();
        assertThat(analytics.averageContributionAmountPaise()).isZero();
        assertThat(analytics.drawCompletionRate()).isZero();
    }
}
