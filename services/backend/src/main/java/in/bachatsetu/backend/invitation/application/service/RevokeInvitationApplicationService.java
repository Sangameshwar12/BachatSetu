package in.bachatsetu.backend.invitation.application.service;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.security.GroupAuthorizationService;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.application.command.RevokeInvitationCommand;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.port.ClockPort;
import in.bachatsetu.backend.invitation.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.application.usecase.RevokeInvitationUseCase;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import java.util.Objects;

public final class RevokeInvitationApplicationService implements RevokeInvitationUseCase {

    private final GroupInvitationRepository invitationRepository;
    private final SavingsGroupRepository groupRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final GroupAuthorizationService authorization = new GroupAuthorizationService();

    public RevokeInvitationApplicationService(
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction) {
        this.invitationRepository = Objects.requireNonNull(invitationRepository, "invitationRepository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @Override
    public void execute(RevokeInvitationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        transaction.execute(() -> {
            SavingsGroup group = groupRepository.findById(command.tenantId(), new GroupId(command.groupId()))
                    .orElseThrow(() -> new InvitationApplicationException(
                            InvitationFailureReason.GROUP_NOT_FOUND, "no group exists for this identifier"));
            authorization.requireOwner(group, command.actorId());

            GroupInvitation invitation = invitationRepository
                    .findActiveByGroup(command.tenantId(), command.groupId())
                    .orElseThrow(() -> new InvitationApplicationException(
                            InvitationFailureReason.NO_ACTIVE_INVITATION, "no active invitation exists for this group"));
            invitation.revoke(command.actorId(), clock.now());
            invitationRepository.save(invitation);
            eventPublisher.publish(invitation.pullDomainEvents());
            return null;
        });
    }
}
