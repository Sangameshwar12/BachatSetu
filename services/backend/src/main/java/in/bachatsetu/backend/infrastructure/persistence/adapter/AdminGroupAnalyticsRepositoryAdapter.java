package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.analytics.model.GroupAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.MonthlyMetric;
import in.bachatsetu.backend.admin.domain.analytics.port.GroupAnalyticsRepository;
import in.bachatsetu.backend.draw.domain.model.DrawStatus;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.DrawSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes savings group analytics from the existing Group, Member, and Draw Spring Data repositories — no
 * SQL view, no caching, no scheduled aggregation.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AdminGroupAnalyticsRepositoryAdapter implements GroupAnalyticsRepository {

    private final SavingsGroupSpringDataRepository groupRepository;
    private final MemberSpringDataRepository memberRepository;
    private final DrawSpringDataRepository drawRepository;

    public AdminGroupAnalyticsRepositoryAdapter(
            SavingsGroupSpringDataRepository groupRepository,
            MemberSpringDataRepository memberRepository,
            DrawSpringDataRepository drawRepository) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "group repository must not be null");
        this.memberRepository = Objects.requireNonNull(memberRepository, "member repository must not be null");
        this.drawRepository = Objects.requireNonNull(drawRepository, "draw repository must not be null");
    }

    @Override
    public GroupAnalytics compute() {
        long totalGroups = groupRepository.countByDeletedFalse();
        long totalMembers = memberRepository.countByDeletedFalse();
        long totalDraws = drawRepository.countByDeletedFalse();
        long completedDraws = drawRepository.countByStatusAndDeletedFalse(DrawStatus.COMPLETED);
        Double averageContribution = groupRepository.findAverageContributionAmountPaise();

        return new GroupAnalytics(
                totalGroups,
                groupRepository.countByStatusAndDeletedFalse(GroupStatus.ACTIVE),
                groupRepository.countByStatusAndDeletedFalse(GroupStatus.CLOSED),
                totalGroups == 0 ? 0.0 : (double) totalMembers / totalGroups,
                averageContribution == null ? 0.0 : averageContribution,
                findMonthlyNewGroups(),
                totalDraws == 0 ? 0.0 : (double) completedDraws / totalDraws);
    }

    private List<MonthlyMetric> findMonthlyNewGroups() {
        return groupRepository.findMonthlyNewGroupCounts().stream()
                .map(row -> new MonthlyMetric(
                        YearMonth.of(toInt(row[0]), toInt(row[1])), ((Number) row[2]).longValue()))
                .toList();
    }

    private static int toInt(Object value) {
        return ((Number) value).intValue();
    }
}
