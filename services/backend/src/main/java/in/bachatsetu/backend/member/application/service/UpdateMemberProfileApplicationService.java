package in.bachatsetu.backend.member.application.service;

import in.bachatsetu.backend.member.application.command.UpdateMemberProfileCommand;
import in.bachatsetu.backend.member.application.mapper.MemberApplicationMapper;
import in.bachatsetu.backend.member.application.port.ClockPort;
import in.bachatsetu.backend.member.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.security.MemberAuthorizationService;
import in.bachatsetu.backend.member.application.usecase.UpdateMemberProfileUseCase;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import java.util.Objects;

/** Coordinates member profile status updates without owning business invariants. */
public final class UpdateMemberProfileApplicationService implements UpdateMemberProfileUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final MemberAuthorizationService authorization;
    private final MemberApplicationSupport support;

    public UpdateMemberProfileApplicationService(
            MemberRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            MemberApplicationMapper mapper,
            MemberAuthorizationService authorization) {
        Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.authorization = Objects.requireNonNull(authorization, "authorization must not be null");
        this.support = new MemberApplicationSupport(repository, eventPublisher, mapper);
    }

    @Override
    public MemberProfileResult execute(UpdateMemberProfileCommand command) {
        Objects.requireNonNull(command, "update command must not be null");
        return transaction.execute(() -> update(command));
    }

    private MemberProfileResult update(UpdateMemberProfileCommand command) {
        MemberProfile member = support.requireMember(command.tenantId(), command.memberId());
        authorization.requireSelf(member, command.actorId());
        member.changeStatus(command.status(), command.actorId(), clock.now());
        return support.saveAndPublish(member);
    }
}
