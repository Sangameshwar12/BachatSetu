package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.model.PlatformGroupStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.SortDirection;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-tenant, platform-wide view of savings groups — deliberately a lean summary (no organizer or member
 * entity graph), unlike the tenant-scoped queries {@code SavingsGroupRepositoryAdapter} uses, since an
 * administrative listing needs only the group's own fields plus its member count.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AdminGroupRepositoryAdapter implements PlatformGroupRepository {

    private final SavingsGroupSpringDataRepository groupRepository;
    private final MemberSpringDataRepository memberRepository;

    public AdminGroupRepositoryAdapter(
            SavingsGroupSpringDataRepository groupRepository, MemberSpringDataRepository memberRepository) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "group repository must not be null");
        this.memberRepository = Objects.requireNonNull(memberRepository, "member repository must not be null");
    }

    @Override
    public PlatformPage<PlatformGroupSummary> search(PlatformGroupSearchCriteria criteria) {
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), toSort(criteria));
        Page<SavingsGroupJpaEntity> page = groupRepository.searchAcrossTenants(
                toGroupStatus(criteria.status()), criteria.createdAfter(), criteria.createdBefore(), pageable);
        return new PlatformPage<>(
                page.getContent().stream().map(this::toSummary).toList(), criteria.page(), criteria.size(),
                page.getTotalElements());
    }

    private PlatformGroupSummary toSummary(SavingsGroupJpaEntity entity) {
        long memberCount = memberRepository.countByGroup_IdAndDeletedFalse(entity.getId());
        return new PlatformGroupSummary(
                new AggregateId(entity.getId()),
                new AggregateId(entity.getTenantId()),
                entity.getCode(),
                entity.getName(),
                PlatformGroupStatus.valueOf(entity.getStatus().name()),
                (int) memberCount,
                entity.getCreatedAt());
    }

    private static GroupStatus toGroupStatus(PlatformGroupStatus status) {
        return status == null ? null : GroupStatus.valueOf(status.name());
    }

    private Sort toSort(PlatformGroupSearchCriteria criteria) {
        Sort.Direction direction =
                criteria.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, "createdAt");
    }
}
