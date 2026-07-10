package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.model.PlatformTenantSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformPageRequest;
import in.bachatsetu.backend.admin.domain.port.PlatformTenantRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives tenant-level totals from the users and groups already recorded per tenant — this codebase has no
 * dedicated Tenant aggregate or table, so a tenant "exists" by having at least one user.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AdminTenantRepositoryAdapter implements PlatformTenantRepository {

    private final UserSpringDataRepository userRepository;
    private final SavingsGroupSpringDataRepository groupRepository;

    public AdminTenantRepositoryAdapter(
            UserSpringDataRepository userRepository, SavingsGroupSpringDataRepository groupRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "user repository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "group repository must not be null");
    }

    @Override
    public PlatformPage<PlatformTenantSummary> search(PlatformPageRequest pageRequest) {
        Page<UUID> tenantIds = userRepository.findDistinctTenantIds(
                PageRequest.of(pageRequest.page(), pageRequest.size()));
        return new PlatformPage<>(
                tenantIds.getContent().stream().map(this::toSummary).toList(), pageRequest.page(),
                pageRequest.size(), tenantIds.getTotalElements());
    }

    private PlatformTenantSummary toSummary(UUID tenantId) {
        long userCount = userRepository.countByTenantIdAndDeletedFalse(tenantId);
        long groupCount = groupRepository.countByTenantIdAndDeletedFalse(tenantId);
        return new PlatformTenantSummary(new AggregateId(tenantId), userCount, groupCount);
    }
}
