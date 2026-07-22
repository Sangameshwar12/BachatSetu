package in.bachatsetu.backend.invitation.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.query.InvitationAcceptedResult;
import in.bachatsetu.backend.invitation.application.query.InvitationPreviewResult;
import in.bachatsetu.backend.invitation.application.usecase.AcceptInvitationUseCase;
import in.bachatsetu.backend.invitation.application.usecase.PreviewInvitationUseCase;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JoinController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({InvitationApiMapper.class, InvitationExceptionHandler.class})
class JoinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PreviewInvitationUseCase previewInvitation;

    @MockBean
    private AcceptInvitationUseCase acceptInvitation;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void previewsAGroupByToken() throws Exception {
        when(previewInvitation.execute("tok3n")).thenReturn(
                new InvitationPreviewResult(
                        "Diwali Bachat Gat", "Asha Rao", 100_000L, "INR", "MONTHLY", 4, 10, "ACTIVE"));

        mockMvc.perform(get("/api/v1/join/{token}", "tok3n"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("Diwali Bachat Gat"))
                .andExpect(jsonPath("$.organizerName").value("Asha Rao"));
    }

    @Test
    void mapsPreviewNotFound() throws Exception {
        when(previewInvitation.execute("missing")).thenThrow(new InvitationApplicationException(
                InvitationFailureReason.INVITATION_NOT_FOUND, "no invitation"));

        mockMvc.perform(get("/api/v1/join/{token}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("invitation-not-found"));
    }

    @Test
    void joinsAGroupByCode() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        AggregateId groupId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        when(acceptInvitation.execute(any())).thenReturn(
                new InvitationAcceptedResult(groupId, memberId, Instant.parse("2026-07-10T06:00:00Z")));

        mockMvc.perform(post("/api/v1/groups/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"AB3D9F2K","channel":"CODE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/groups/" + groupId + "/members/" + memberId))
                .andExpect(jsonPath("$.groupId").value(groupId.toString()));
    }

    @Test
    void reportsAConcurrentJoinRaceAsConflict() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(acceptInvitation.execute(any())).thenThrow(
                new ObjectOptimisticLockingFailureException("SavingsGroup", "1"));

        mockMvc.perform(post("/api/v1/groups/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"AB3D9F2K","channel":"CODE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("concurrent-modification"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
