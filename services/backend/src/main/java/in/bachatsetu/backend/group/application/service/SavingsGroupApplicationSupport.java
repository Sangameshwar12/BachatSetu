package in.bachatsetu.backend.group.application.service;

import in.bachatsetu.backend.group.application.exception.SavingsGroupNotFoundException;
import in.bachatsetu.backend.group.application.mapper.SavingsGroupApplicationMapper;
import in.bachatsetu.backend.group.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Shared persistence and event-publication mechanics for command services. */
final class SavingsGroupApplicationSupport {

    private final SavingsGroupRepository repository;
    private final DomainEventPublisherPort eventPublisher;
    private final SavingsGroupApplicationMapper mapper;

    SavingsGroupApplicationSupport(
            SavingsGroupRepository repository,
            DomainEventPublisherPort eventPublisher,
            SavingsGroupApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    SavingsGroup requireGroup(AggregateId tenantId, GroupId groupId) {
        return repository.findById(tenantId, groupId)
                .orElseThrow(() -> new SavingsGroupNotFoundException("savings group does not exist"));
    }

    SavingsGroupResult saveAndPublish(SavingsGroup group) {
        repository.save(group);
        eventPublisher.publish(group.pullDomainEvents());
        return mapper.toResult(group);
    }
}
