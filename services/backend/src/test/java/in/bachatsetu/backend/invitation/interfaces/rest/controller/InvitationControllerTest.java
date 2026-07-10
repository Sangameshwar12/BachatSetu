package in.bachatsetu.backend.invitation.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.query.InvitationResult;
import in.bachatsetu.backend.invitation.application.usecase.CreateInvitationUseCase;
import in.bachatsetu.backend.invitation.application.usecase.GetCurrentInvitationUseCase;
import in.bachatsetu.backend.invitation.application.usecase.RevokeInvitationUseCase;
import in.bachatsetu.backend.invitation.domain.model.InvitationStatus;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.invitation.interfaces.rest.exception.InvitationExceptionHandler;
import in.bachatsetu.backend.invitation.interfaces.rest.mapper.InvitationApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvitationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({InvitationApiMapper.class, InvitationExceptionHandler.class})
class InvitationControllerTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-07-16T06:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateInvitationUseCase createInvitation;

    @MockBean
    private GetCurrentInvitationUseCase getCurrentInvitation;

    @MockBean
    private RevokeInvitationUseCase revokeInvitation;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsAnInvitation() throws Exception {
        AggregateId groupId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(createInvitation.execute(any())).thenReturn(new InvitationResult(
                AggregateId.newId(), groupId, "AB3D9F2K", "tok3n", InvitationType.CODE, InvitationStatus.ACTIVE,
                EXPIRES_AT));

        mockMvc.perform(post("/api/v1/groups/{groupId}/invite", groupId.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"CODE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AB3D9F2K"))
                .andExpect(jsonPath("$.joinLink").value("/join/tok3n"));
    }

    @Test
    void mapsGroupNotFoundOnCreate() throws Exception {
        AggregateId groupId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(createInvitation.execute(any())).thenThrow(new InvitationApplicationException(
                InvitationFailureReason.GROUP_NOT_FOUND, "no group"));

        mockMvc.perform(post("/api/v1/groups/{groupId}/invite", groupId.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"CODE"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("group-not-found"));
    }

    @Test
    void viewsTheCurrentInvitation() throws Exception {
        AggregateId groupId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getCurrentInvitation.execute(any(), any(), any())).thenReturn(new InvitationResult(
                AggregateId.newId(), groupId, "AB3D9F2K", "tok3n", InvitationType.CODE, InvitationStatus.ACTIVE,
                EXPIRES_AT));

        mockMvc.perform(get("/api/v1/groups/{groupId}/invite", groupId.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void revokesTheCurrentInvitation() throws Exception {
        AggregateId groupId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(delete("/api/v1/groups/{groupId}/invite", groupId.value()))
                .andExpect(status().isNoContent());
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
