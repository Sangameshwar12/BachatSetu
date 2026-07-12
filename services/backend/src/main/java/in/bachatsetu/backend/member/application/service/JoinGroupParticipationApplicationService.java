package in.bachatsetu.backend.member.application.service;

import in.bachatsetu.backend.member.application.command.JoinGroupParticipationCommand;
import in.bachatsetu.backend.member.application.mapper.MemberApplicationMapper;
import in.bachatsetu.backend.member.application.port.ClockPort;
import in.bachatsetu.backend.member.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.security.MemberAuthorizationService;
import in.bachatsetu.backend.member.application.usecase.JoinGroupParticipationUseCase;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import java.util.Objects;

/** Coordinates adding a further group participation to an existing member profile. */
public final class JoinGroupParticipationApplicationService implements JoinGroupParticipationUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final MemberApplicationSupport support;
    private final MemberAuthorizationService authorization;

    public JoinGroupParticipationApplicationService(
            MemberRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            MemberApplicationMapper mapper,
            MemberAuthorizationService authorization) {
        Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.support = new MemberApplicationSupport(repository, eventPublisher, mapper);
        this.authorization = Objects.requireNonNull(authorization, "authorization must not be null");
    }

    @Override
    public MemberProfileResult execute(JoinGroupParticipationCommand command) {
        Objects.requireNonNull(command, "join command must not be null");
        return transaction.execute(() -> join(command));
    }

    private MemberProfileResult join(JoinGroupParticipationCommand command) {
        MemberProfile member = support.requireMember(command.tenantId(), command.memberId());
        authorization.requireSelf(member, command.actorId());
        member.joinGroup(command.groupId(), command.role(), command.actorId(), clock.now());
        return support.saveAndPublish(member);
    }
}
