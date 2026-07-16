package in.bachatsetu.backend.invitation.application.service;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.application.query.InvitationPreviewResult;
import in.bachatsetu.backend.invitation.application.usecase.PreviewInvitationUseCase;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.util.Objects;

/** Builds a safe, pre-join preview from a QR/link token without exposing raw identifiers. */
public final class PreviewInvitationApplicationService implements PreviewInvitationUseCase {

    private final GroupInvitationRepository invitationRepository;
    private final SavingsGroupRepository groupRepository;
    private final UserRepository userProfileRepository;
    private final TransactionPort transaction;

    public PreviewInvitationApplicationService(
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            UserRepository userProfileRepository,
            TransactionPort transaction) {
        this.invitationRepository = Objects.requireNonNull(invitationRepository, "invitationRepository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.userProfileRepository =
                Objects.requireNonNull(userProfileRepository, "userProfileRepository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @Override
    public InvitationPreviewResult execute(String token) {
        Objects.requireNonNull(token, "token must not be null");
        return transaction.execute(() -> {
            GroupInvitation invitation = invitationRepository.findByToken(new InvitationToken(token))
                    .orElseThrow(() -> new InvitationApplicationException(
                            InvitationFailureReason.INVITATION_NOT_FOUND, "no invitation exists for this token"));
            SavingsGroup group = groupRepository.findById(invitation.tenantId(), new GroupId(invitation.groupId()))
                    .orElseThrow(() -> new InvitationApplicationException(
                            InvitationFailureReason.GROUP_NOT_FOUND, "no group exists for this invitation"));
            String organizerName = userProfileRepository.findById(group.ownerId().value())
                    .map(UserProfile::name)
                    .map(in.bachatsetu.backend.user.domain.model.PersonName::displayName)
                    .orElse("Group organizer");
            return new InvitationPreviewResult(
                    group.name().value(),
                    organizerName,
                    group.contributionAmount().value().minorUnits(),
                    group.contributionAmount().value().currency().getCurrencyCode(),
                    group.rule().contributionSchedule().frequency().name(),
                    group.members().size(),
                    group.maximumMembers().value(),
                    group.status().name());
        });
    }
}
