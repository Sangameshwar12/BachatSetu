package in.bachatsetu.backend.invitation.application.service;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.security.GroupAuthorizationService;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.application.query.InvitationResult;
import in.bachatsetu.backend.invitation.application.usecase.GetCurrentInvitationUseCase;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public final class GetCurrentInvitationApplicationService implements GetCurrentInvitationUseCase {

    private final GroupInvitationRepository invitationRepository;
    private final SavingsGroupRepository groupRepository;
    private final TransactionPort transaction;
    private final GroupAuthorizationService authorization = new GroupAuthorizationService();

    public GetCurrentInvitationApplicationService(
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            TransactionPort transaction) {
        this.invitationRepository = Objects.requireNonNull(invitationRepository, "invitationRepository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @Override
    public InvitationResult execute(AggregateId tenantId, AggregateId groupId, AggregateId actorId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        return transaction.execute(() -> {
            SavingsGroup group = groupRepository.findById(tenantId, new GroupId(groupId))
                    .orElseThrow(() -> new InvitationApplicationException(
                            InvitationFailureReason.GROUP_NOT_FOUND, "no group exists for this identifier"));
            authorization.requireOwner(group, actorId);
            GroupInvitation invitation = invitationRepository.findActiveByGroup(tenantId, groupId)
                    .orElseThrow(() -> new InvitationApplicationException(
                            InvitationFailureReason.NO_ACTIVE_INVITATION, "no active invitation exists for this group"));
            return toResult(invitation);
        });
    }

    private InvitationResult toResult(GroupInvitation invitation) {
        return new InvitationResult(
                invitation.id(), invitation.groupId(), invitation.code().value(), invitation.token().value(),
                invitation.type(), invitation.status(), invitation.expiresAt());
    }
}
