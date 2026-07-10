package in.bachatsetu.backend.invitation.application.service;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.application.command.AcceptInvitationCommand;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.port.ClockPort;
import in.bachatsetu.backend.invitation.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.application.query.InvitationAcceptedResult;
import in.bachatsetu.backend.invitation.application.usecase.AcceptInvitationUseCase;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates and accepts an invitation, then joins the caller to the referenced group through the
 * pre-existing {@link SavingsGroup#joinMember} aggregate method — this is a self-service join, unlike
 * {@code JoinGroupUseCase}, which requires the actor to already be the group owner (used when an
 * organizer adds a member directly). Both aggregates are only saved once every business rule has
 * passed, so a rejected join never consumes the invitation.
 */
public final class AcceptInvitationApplicationService implements AcceptInvitationUseCase {

    private final GroupInvitationRepository invitationRepository;
    private final SavingsGroupRepository groupRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final in.bachatsetu.backend.group.application.port.DomainEventPublisherPort groupEventPublisher;
    private final ClockPort clock;
    private final TransactionPort transaction;

    public AcceptInvitationApplicationService(
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            in.bachatsetu.backend.group.application.port.DomainEventPublisherPort groupEventPublisher,
            ClockPort clock,
            TransactionPort transaction) {
        this.invitationRepository = Objects.requireNonNull(invitationRepository, "invitationRepository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.groupEventPublisher =
                Objects.requireNonNull(groupEventPublisher, "groupEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @Override
    public InvitationAcceptedResult execute(AcceptInvitationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return transaction.execute(() -> {
            GroupInvitation invitation = resolveInvitation(command);
            SavingsGroup group = groupRepository.findById(invitation.tenantId(), new GroupId(invitation.groupId()))
                    .orElseThrow(() -> new InvitationApplicationException(
                            InvitationFailureReason.GROUP_NOT_FOUND, "no group exists for this invitation"));
            Instant now = clock.now();

            invitation.accept(command.actorId(), command.channel(), now);
            group.joinMember(command.actorId(), command.actorId(), now);

            invitationRepository.save(invitation);
            eventPublisher.publish(invitation.pullDomainEvents());
            groupRepository.save(group);
            groupEventPublisher.publish(group.pullDomainEvents());

            return new InvitationAcceptedResult(invitation.groupId(), command.actorId(), now);
        });
    }

    private GroupInvitation resolveInvitation(AcceptInvitationCommand command) {
        Optional<GroupInvitation> found = command.code() != null && !command.code().isBlank()
                ? invitationRepository.findByCode(command.tenantId(), new InvitationCode(command.code()))
                : invitationRepository.findByToken(new InvitationToken(command.token()));
        return found.orElseThrow(() -> new InvitationApplicationException(
                InvitationFailureReason.INVITATION_NOT_FOUND, "no invitation exists for this code or token"));
    }
}
