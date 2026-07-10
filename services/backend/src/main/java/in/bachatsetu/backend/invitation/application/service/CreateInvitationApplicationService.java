package in.bachatsetu.backend.invitation.application.service;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.security.GroupAuthorizationService;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.application.command.CreateInvitationCommand;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.port.ClockPort;
import in.bachatsetu.backend.invitation.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.invitation.application.port.InvitationCodeGeneratorPort;
import in.bachatsetu.backend.invitation.application.port.InvitationTokenGeneratorPort;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.application.query.InvitationResult;
import in.bachatsetu.backend.invitation.application.usecase.CreateInvitationUseCase;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Generates the group's current invitation, replacing (revoking) any prior active one. */
public final class CreateInvitationApplicationService implements CreateInvitationUseCase {

    private final GroupInvitationRepository invitationRepository;
    private final SavingsGroupRepository groupRepository;
    private final InvitationCodeGeneratorPort codeGenerator;
    private final InvitationTokenGeneratorPort tokenGenerator;
    private final DomainEventPublisherPort eventPublisher;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final Duration validity;
    private final GroupAuthorizationService authorization = new GroupAuthorizationService();

    public CreateInvitationApplicationService(
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            InvitationCodeGeneratorPort codeGenerator,
            InvitationTokenGeneratorPort tokenGenerator,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            Duration validity) {
        this.invitationRepository = Objects.requireNonNull(invitationRepository, "invitationRepository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator must not be null");
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator, "tokenGenerator must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.validity = Objects.requireNonNull(validity, "validity must not be null");
    }

    @Override
    public InvitationResult execute(CreateInvitationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return transaction.execute(() -> {
            SavingsGroup group = groupRepository.findById(command.tenantId(), new GroupId(command.groupId()))
                    .orElseThrow(() -> new InvitationApplicationException(
                            InvitationFailureReason.GROUP_NOT_FOUND, "no group exists for this identifier"));
            authorization.requireOwner(group, command.actorId());

            invitationRepository.findActiveByGroup(command.tenantId(), command.groupId()).ifPresent(existing -> {
                existing.revoke(command.actorId(), clock.now());
                invitationRepository.save(existing);
                eventPublisher.publish(existing.pullDomainEvents());
            });

            Instant now = clock.now();
            GroupInvitation invitation = GroupInvitation.create(
                    AggregateId.newId(), command.tenantId(), command.groupId(), codeGenerator.generate(),
                    tokenGenerator.generate(), command.type(), now.plus(validity), command.actorId(), now);
            invitationRepository.save(invitation);
            eventPublisher.publish(invitation.pullDomainEvents());
            return toResult(invitation);
        });
    }

    private InvitationResult toResult(GroupInvitation invitation) {
        return new InvitationResult(
                invitation.id(), invitation.groupId(), invitation.code().value(), invitation.token().value(),
                invitation.type(), invitation.status(), invitation.expiresAt());
    }
}
