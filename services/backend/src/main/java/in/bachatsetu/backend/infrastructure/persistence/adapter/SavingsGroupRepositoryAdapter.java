package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.port.GroupPage;
import in.bachatsetu.backend.group.application.port.GroupPageRequest;
import in.bachatsetu.backend.group.application.port.GroupSortField;
import in.bachatsetu.backend.group.application.port.SortDirection;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceConflictException;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.SavingsGroupJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Persistence adapter for both current and legacy Savings Group repository ports. */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class SavingsGroupRepositoryAdapter implements SavingsGroupRepository, GroupRepository {

    private final SavingsGroupSpringDataRepository repository;
    private final SavingsGroupJpaMapper mapper;
    private final JpaReferenceProvider references;

    public SavingsGroupRepositoryAdapter(
            SavingsGroupSpringDataRepository repository,
            SavingsGroupJpaMapper mapper,
            JpaReferenceProvider references) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.references = Objects.requireNonNull(references, "reference provider must not be null");
    }

    @Override
    @Transactional
    public void save(SavingsGroup group) {
        Objects.requireNonNull(group, "savings group must not be null");
        RepositoryOperations.execute(() -> {
            SavingsGroupJpaEntity entity = repository.findById(group.id().value())
                    .map(existing -> updateExisting(group, existing))
                    .orElseGet(() -> mapper.toEntity(group, references));
            repository.save(entity);
            return null;
        });
    }

    @Override
    public Optional<SavingsGroup> findById(AggregateId tenantId, GroupId groupId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        return repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), groupId.value().value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<SavingsGroup> findByGroupCode(AggregateId tenantId, GroupCode groupCode) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupCode, "group code must not be null");
        return repository.findByTenantIdAndCodeAndDeletedFalse(tenantId.value(), groupCode.value())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByGroupCode(AggregateId tenantId, GroupCode groupCode) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupCode, "group code must not be null");
        return repository.existsByTenantIdAndCodeAndDeletedFalse(tenantId.value(), groupCode.value());
    }

    @Override
    public List<SavingsGroup> findByOwnerId(AggregateId tenantId, AggregateId ownerId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(ownerId, "owner id must not be null");
        return repository.findAllByTenantIdAndOrganizer_IdAndDeletedFalse(tenantId.value(), ownerId.value())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public GroupPage<SavingsGroup> findPage(AggregateId tenantId, GroupPageRequest pageRequest) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        Pageable pageable = PageRequest.of(pageRequest.page(), pageRequest.size(), toSort(pageRequest));
        Page<UUID> idPage = repository.findPageIdsByTenantIdAndOptionalStatus(
                tenantId.value(), pageRequest.statusFilter(), pageable);
        List<UUID> orderedIds = idPage.getContent();
        Map<UUID, SavingsGroupJpaEntity> byId = repository.findByIdInAndDeletedFalse(orderedIds).stream()
                .collect(Collectors.toMap(SavingsGroupJpaEntity::getId, Function.identity()));
        List<SavingsGroup> content = orderedIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(mapper::toDomain)
                .toList();
        return new GroupPage<>(content, idPage.getNumber(), idPage.getSize(), idPage.getTotalElements());
    }

    private Sort toSort(GroupPageRequest pageRequest) {
        String property = toSortProperty(pageRequest.sortField());
        Sort.Direction direction = pageRequest.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private String toSortProperty(GroupSortField sortField) {
        if (sortField == GroupSortField.NAME) {
            return "name";
        }
        return "createdAt";
    }

    @Override
    @Transactional
    public void delete(AggregateId tenantId, GroupId groupId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        RepositoryOperations.execute(() -> {
            repository.softDelete(tenantId.value(), groupId.value().value(), Instant.now());
            return null;
        });
    }

    @Override
    public Optional<SavingsGroup> findById(AggregateId groupId) {
        Objects.requireNonNull(groupId, "group id must not be null");
        return repository.findByIdAndDeletedFalse(groupId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<SavingsGroup> findByCode(AggregateId tenantId, GroupCode code) {
        return findByGroupCode(tenantId, code);
    }

    private SavingsGroupJpaEntity updateExisting(
            SavingsGroup group,
            SavingsGroupJpaEntity existing) {
        if (group.version() != existing.getVersion() + 1) {
            throw new PersistenceConflictException("savings group version is stale");
        }
        return mapper.updateEntity(group, existing, references);
    }
}
