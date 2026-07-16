package in.bachatsetu.backend.invitation.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.invitation.application.command.AcceptInvitationCommand;
import in.bachatsetu.backend.invitation.application.command.CreateInvitationCommand;
import in.bachatsetu.backend.invitation.application.command.RevokeInvitationCommand;
import in.bachatsetu.backend.invitation.application.query.InvitationAcceptedResult;
import in.bachatsetu.backend.invitation.application.query.InvitationPreviewResult;
import in.bachatsetu.backend.invitation.application.query.InvitationResult;
import in.bachatsetu.backend.invitation.application.usecase.GetCurrentInvitationUseCase;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.AcceptInvitationRequest;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.AcceptInvitationResponse;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.CreateInvitationRequest;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.InvitationPreviewResponse;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.InvitationResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to invitation application commands and safe responses. */
@Component
public class InvitationApiMapper {

    private static final String JOIN_LINK_PREFIX = "/join/";

    public CreateInvitationCommand toCommand(
            String groupId, AuthenticatedUser currentUser, CreateInvitationRequest request) {
        return new CreateInvitationCommand(
                currentUser.tenantId(), new AggregateId(UUID.fromString(groupId)),
                InvitationType.valueOf(request.type()), currentUser.userId().toAggregateId());
    }

    public RevokeInvitationCommand toRevokeCommand(String groupId, AuthenticatedUser currentUser) {
        return new RevokeInvitationCommand(
                currentUser.tenantId(), new AggregateId(UUID.fromString(groupId)),
                currentUser.userId().toAggregateId());
    }

    public InvitationResult current(GetCurrentInvitationUseCase useCase, String groupId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(useCase, "use case must not be null");
        return useCase.execute(
                currentUser.tenantId(), new AggregateId(UUID.fromString(groupId)),
                currentUser.userId().toAggregateId());
    }

    public InvitationResponse toResponse(InvitationResult result) {
        return new InvitationResponse(
                result.invitationId().toString(),
                result.groupId().toString(),
                result.code(),
                JOIN_LINK_PREFIX + result.token(),
                result.type().name(),
                result.status().name(),
                result.expiresAt());
    }

    public InvitationPreviewResponse toResponse(InvitationPreviewResult result) {
        return new InvitationPreviewResponse(
                result.groupName(), result.organizerName(), result.contributionAmountPaise(), result.currencyCode(),
                result.frequency(), result.memberCount(), result.maximumMembers(), result.status());
    }

    public AcceptInvitationCommand toCommand(AuthenticatedUser currentUser, AcceptInvitationRequest request) {
        return new AcceptInvitationCommand(
                currentUser.tenantId(), request.code(), request.token(), InvitationType.valueOf(request.channel()),
                currentUser.userId().toAggregateId());
    }

    public AcceptInvitationResponse toResponse(InvitationAcceptedResult result) {
        return new AcceptInvitationResponse(
                result.groupId().toString(), result.memberId().toString(), result.joinedAt());
    }

    /** Location header for the newly-created membership — kept here so the controller never touches domain types. */
    public URI toLocationUri(InvitationAcceptedResult result) {
        return URI.create("/api/v1/groups/" + result.groupId() + "/members/" + result.memberId());
    }
}
