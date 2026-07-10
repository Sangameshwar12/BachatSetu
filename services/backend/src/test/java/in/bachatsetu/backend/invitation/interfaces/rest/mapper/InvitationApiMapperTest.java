package in.bachatsetu.backend.invitation.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.invitation.application.command.AcceptInvitationCommand;
import in.bachatsetu.backend.invitation.application.command.CreateInvitationCommand;
import in.bachatsetu.backend.invitation.application.command.RevokeInvitationCommand;
import in.bachatsetu.backend.invitation.application.query.InvitationAcceptedResult;
import in.bachatsetu.backend.invitation.application.query.InvitationPreviewResult;
import in.bachatsetu.backend.invitation.application.query.InvitationResult;
import in.bachatsetu.backend.invitation.application.usecase.GetCurrentInvitationUseCase;
import in.bachatsetu.backend.invitation.domain.model.InvitationStatus;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.AcceptInvitationRequest;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.AcceptInvitationResponse;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.CreateInvitationRequest;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.InvitationPreviewResponse;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.InvitationResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvitationApiMapperTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-07-16T06:00:00Z");

    private final InvitationApiMapper mapper = new InvitationApiMapper();

    @Test
    void mapsCreateRequestToCommand() {
        AuthenticatedUser currentUser = authenticatedUser();
        AggregateId groupId = AggregateId.newId();

        CreateInvitationCommand command =
                mapper.toCommand(groupId.value().toString(), currentUser, new CreateInvitationRequest("CODE"));

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.groupId()).isEqualTo(groupId);
        assertThat(command.type()).isEqualTo(InvitationType.CODE);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void mapsRevokeRequestToCommand() {
        AuthenticatedUser currentUser = authenticatedUser();
        AggregateId groupId = AggregateId.newId();

        RevokeInvitationCommand command = mapper.toRevokeCommand(groupId.value().toString(), currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.groupId()).isEqualTo(groupId);
    }

    @Test
    void delegatesCurrentLookupToTheUseCase() {
        AuthenticatedUser currentUser = authenticatedUser();
        AggregateId groupId = AggregateId.newId();
        GetCurrentInvitationUseCase useCase = mock(GetCurrentInvitationUseCase.class);
        InvitationResult expected = new InvitationResult(
                AggregateId.newId(), groupId, "AB3D9F2K", "a".repeat(43), InvitationType.CODE,
                InvitationStatus.ACTIVE, EXPIRES_AT);
        when(useCase.execute(any(), any(), any())).thenReturn(expected);

        InvitationResult result = mapper.current(useCase, groupId.value().toString(), currentUser);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(currentUser.tenantId(), groupId, currentUser.userId().toAggregateId());
    }

    @Test
    void mapsInvitationResultToResponseWithAJoinLink() {
        InvitationResult result = new InvitationResult(
                AggregateId.newId(), AggregateId.newId(), "AB3D9F2K", "tok3n", InvitationType.LINK,
                InvitationStatus.ACTIVE, EXPIRES_AT);

        InvitationResponse response = mapper.toResponse(result);

        assertThat(response.code()).isEqualTo("AB3D9F2K");
        assertThat(response.joinLink()).isEqualTo("/join/tok3n");
        assertThat(response.type()).isEqualTo("LINK");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void mapsPreviewResultToResponse() {
        InvitationPreviewResult result =
                new InvitationPreviewResult("Diwali Bachat Gat", "Asha Rao", 100_000L, "INR", "MONTHLY", 4, 10);

        InvitationPreviewResponse response = mapper.toResponse(result);

        assertThat(response.groupName()).isEqualTo("Diwali Bachat Gat");
        assertThat(response.organizerName()).isEqualTo("Asha Rao");
        assertThat(response.memberCount()).isEqualTo(4);
    }

    @Test
    void mapsAcceptRequestToCommand() {
        AuthenticatedUser currentUser = authenticatedUser();
        AcceptInvitationRequest request = new AcceptInvitationRequest("AB3D9F2K", null, "CODE");

        AcceptInvitationCommand command = mapper.toCommand(currentUser, request);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.code()).isEqualTo("AB3D9F2K");
        assertThat(command.channel()).isEqualTo(InvitationType.CODE);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void mapsAcceptedResultToResponse() {
        AggregateId groupId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        InvitationAcceptedResult result = new InvitationAcceptedResult(groupId, memberId, EXPIRES_AT);

        AcceptInvitationResponse response = mapper.toResponse(result);

        assertThat(response.groupId()).isEqualTo(groupId.toString());
        assertThat(response.memberId()).isEqualTo(memberId.toString());
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                new UserId(UUID.randomUUID()), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(),
                Set.of());
    }
}
