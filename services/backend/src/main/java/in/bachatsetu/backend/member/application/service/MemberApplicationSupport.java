package in.bachatsetu.backend.member.application.service;

import in.bachatsetu.backend.member.application.exception.MemberProfileNotFoundException;
import in.bachatsetu.backend.member.application.mapper.MemberApplicationMapper;
import in.bachatsetu.backend.member.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Shared persistence and event-publication mechanics for command services. */
final class MemberApplicationSupport {

    private final MemberRepository repository;
    private final DomainEventPublisherPort eventPublisher;
    private final MemberApplicationMapper mapper;

    MemberApplicationSupport(
            MemberRepository repository,
            DomainEventPublisherPort eventPublisher,
            MemberApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    MemberProfile requireMember(AggregateId tenantId, AggregateId memberId) {
        return repository.findById(tenantId, memberId)
                .orElseThrow(() -> new MemberProfileNotFoundException("member profile does not exist"));
    }

    MemberProfileResult saveAndPublish(MemberProfile member) {
        repository.save(member);
        eventPublisher.publish(member.pullDomainEvents());
        return mapper.toResult(member);
    }
}
