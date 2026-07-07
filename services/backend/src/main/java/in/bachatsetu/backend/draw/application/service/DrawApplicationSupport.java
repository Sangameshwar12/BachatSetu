package in.bachatsetu.backend.draw.application.service;

import in.bachatsetu.backend.draw.application.exception.DrawAccessDeniedException;
import in.bachatsetu.backend.draw.application.exception.DrawNotFoundException;
import in.bachatsetu.backend.draw.application.mapper.DrawApplicationMapper;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Shared persistence and event-publication mechanics for command services. */
final class DrawApplicationSupport {

    private final DrawRepository repository;
    private final SavingsGroupRepository groupRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final DrawApplicationMapper mapper;

    DrawApplicationSupport(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            DrawApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "group repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    Draw requireDraw(AggregateId tenantId, AggregateId drawId) {
        return repository.findById(tenantId, drawId)
                .orElseThrow(() -> new DrawNotFoundException("draw does not exist"));
    }

    SavingsGroup requireOwningGroup(AggregateId tenantId, AggregateId groupId) {
        return groupRepository.findById(tenantId, new GroupId(groupId))
                .orElseThrow(() -> new DrawAccessDeniedException("only the group owner may perform this operation"));
    }

    DrawResult saveAndPublish(Draw draw) {
        repository.save(draw);
        eventPublisher.publish(draw.pullDomainEvents());
        return mapper.toResult(draw);
    }
}
