package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.support.SupportTicketJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.SupportTicketJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SupportTicketSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.SortDirection;
import in.bachatsetu.backend.support.domain.model.SupportTicket;
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import in.bachatsetu.backend.support.domain.port.SupportTicketSearchCriteria;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Cross-tenant persistence adapter for {@link SupportTicket} — platform support spans every tenant. */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class SupportTicketRepositoryAdapter implements SupportTicketRepository {

    private final SupportTicketSpringDataRepository repository;
    private final SupportTicketJpaMapper mapper;

    public SupportTicketRepositoryAdapter(SupportTicketSpringDataRepository repository, SupportTicketJpaMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public void save(SupportTicket ticket) {
        RepositoryOperations.execute(() -> {
            Optional<SupportTicketJpaEntity> existing = repository.findById(ticket.id().value());
            SupportTicketJpaEntity candidate = mapper.toEntity(ticket);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }

    @Override
    public Optional<SupportTicket> findById(AggregateId ticketId) {
        return repository.findByIdAndDeletedFalse(ticketId.value()).map(mapper::toDomain);
    }

    @Override
    public Page<SupportTicket> search(SupportTicketSearchCriteria criteria) {
        Sort.Direction direction =
                criteria.direction() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        org.springframework.data.domain.Page<SupportTicketJpaEntity> page = repository.search(
                criteria.status(), criteria.priority(), criteria.category(),
                criteria.tenantId() == null ? null : criteria.tenantId().value(),
                criteria.raisedBy() == null ? null : criteria.raisedBy().value(),
                criteria.createdAfter(), criteria.createdBefore(),
                PageRequest.of(criteria.page(), criteria.size(), Sort.by(direction, "createdAt")));
        return new Page<>(
                page.getContent().stream().map(mapper::toDomain).toList(), criteria.page(), criteria.size(),
                page.getTotalElements());
    }
}
