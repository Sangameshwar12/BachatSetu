package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.audit.domain.port.AuditRepository;
import in.bachatsetu.backend.audit.domain.port.AuditSearchCriteria;
import in.bachatsetu.backend.audit.domain.port.SortDirection;
import in.bachatsetu.backend.infrastructure.persistence.entity.audit.AuditEntryJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.AuditEntryJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.AuditEntrySpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AuditEntryRepositoryAdapter implements AuditRepository {

    private final AuditEntrySpringDataRepository repository;
    private final AuditEntryJpaMapper mapper;

    public AuditEntryRepositoryAdapter(AuditEntrySpringDataRepository repository, AuditEntryJpaMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<AuditEntry> findById(AggregateId tenantId, AggregateId auditId) {
        Objects.requireNonNull(auditId, "audit id must not be null");
        return repository.findByIdAndDeletedFalse(auditId.value())
                .map(mapper::toDomain)
                .filter(entry -> Objects.equals(entry.tenantId(), tenantId));
    }

    @Override
    public AuditPage<AuditEntry> search(AuditSearchCriteria criteria) {
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), toSort(criteria));
        Page<AuditEntryJpaEntity> page = repository.search(
                value(criteria.tenantId()), value(criteria.actorId()), criteria.moduleName(),
                criteria.eventType(), criteria.dateFrom(), criteria.dateTo(), pageable);
        List<AuditEntry> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new AuditPage<>(content, criteria.page(), criteria.size(), page.getTotalElements());
    }

    @Override
    @Transactional
    public void save(AuditEntry entry) {
        RepositoryOperations.execute(() -> {
            Optional<AuditEntryJpaEntity> existing = repository.findById(entry.id().value());
            AuditEntryJpaEntity candidate = mapper.toEntity(entry);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }

    private Sort toSort(AuditSearchCriteria criteria) {
        Sort.Direction direction = criteria.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, "occurredAt");
    }

    private static UUID value(AggregateId id) {
        return id == null ? null : id.value();
    }
}
