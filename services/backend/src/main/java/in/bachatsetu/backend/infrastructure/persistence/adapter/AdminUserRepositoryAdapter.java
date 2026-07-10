package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformUserSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformUserRepository;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSortField;
import in.bachatsetu.backend.admin.domain.port.SortDirection;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-tenant, platform-wide view of the shared user record — deliberately independent of {@code
 * auth.domain.port.UserRepository} and {@code user.domain.port.UserRepository}, both of which are always
 * scoped to a single tenant and therefore cannot serve platform administration.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AdminUserRepositoryAdapter implements PlatformUserRepository {

    private final UserSpringDataRepository repository;

    public AdminUserRepositoryAdapter(UserSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public PlatformPage<PlatformUserSummary> search(PlatformUserSearchCriteria criteria) {
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), toSort(criteria));
        Page<UserJpaEntity> page = repository.searchAcrossTenants(
                toAuthStatus(criteria.status()), criteria.email(), criteria.phoneNumber(), criteria.createdAfter(),
                criteria.createdBefore(), pageable);
        return new PlatformPage<>(
                page.getContent().stream().map(this::toSummary).toList(), criteria.page(), criteria.size(),
                page.getTotalElements());
    }

    @Override
    public Optional<PlatformUserSummary> findById(AggregateId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return repository.findByIdAndDeletedFalse(userId.value()).map(this::toSummary);
    }

    @Override
    @Transactional
    public boolean updateStatus(
            AggregateId userId, PlatformUserStatus status, AggregateId administratorId, Instant at) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(administratorId, "administratorId must not be null");
        Objects.requireNonNull(at, "at must not be null");
        int updated = repository.updateAuthenticationStatus(
                userId.value(), toAuthStatus(status), administratorId.value(), at);
        return updated > 0;
    }

    private PlatformUserSummary toSummary(UserJpaEntity entity) {
        return new PlatformUserSummary(
                new AggregateId(entity.getId()),
                new AggregateId(entity.getTenantId()),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getGivenName(),
                entity.getFamilyName(),
                toPlatformStatus(entity.getAuthenticationStatus()),
                entity.getCreatedAt());
    }

    private static UserStatus toAuthStatus(PlatformUserStatus status) {
        return status == null ? null : UserStatus.valueOf(status.name());
    }

    private static PlatformUserStatus toPlatformStatus(UserStatus status) {
        return status == null ? PlatformUserStatus.PENDING_VERIFICATION : PlatformUserStatus.valueOf(status.name());
    }

    private Sort toSort(PlatformUserSearchCriteria criteria) {
        Sort.Direction direction =
                criteria.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, toSortProperty(criteria.sortField()));
    }

    private String toSortProperty(PlatformUserSortField sortField) {
        return switch (sortField) {
            case CREATED_AT -> "createdAt";
            case FIRST_NAME -> "givenName";
            case LAST_NAME -> "familyName";
            case EMAIL -> "email";
        };
    }
}
