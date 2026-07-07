package in.bachatsetu.backend.draw.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.draw.application.exception.DrawAccessDeniedException;
import in.bachatsetu.backend.draw.application.exception.DrawNotFoundException;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.query.DrawSummary;
import in.bachatsetu.backend.draw.application.usecase.CloseDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.ConductDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.CreateDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.GetDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.ListDrawsUseCase;
import in.bachatsetu.backend.draw.domain.exception.InvalidDrawStateException;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawSortField;
import in.bachatsetu.backend.draw.domain.port.SortDirection;
import in.bachatsetu.backend.draw.interfaces.rest.exception.DrawExceptionHandler;
import in.bachatsetu.backend.draw.interfaces.rest.mapper.DrawApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DrawController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({DrawApiMapper.class, DrawExceptionHandler.class})
class DrawControllerTest {

    private static final AggregateId TENANT_ID = AggregateId.newId();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateDrawUseCase createDraw;

    @MockBean
    private GetDrawUseCase getDraw;

    @MockBean
    private ListDrawsUseCase listDraws;

    @MockBean
    private ConductDrawUseCase conductDraw;

    @MockBean
    private CloseDrawUseCase closeDraw;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsDraw() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID drawId = UUID.randomUUID();
        when(createDraw.execute(any())).thenReturn(result(drawId, "SCHEDULED"));

        mockMvc.perform(post("/api/v1/draws")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/draws/" + drawId))
                .andExpect(jsonPath("$.drawId").value(drawId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void rejectsInvalidGroupIdFormat() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/draws")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": "not-a-uuid", "cycleId": "%s", "drawNumber": 1,
                                 "type": "AUCTION", "scheduledAt": "2026-08-01T10:00:00Z"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void reportsNonOwnerCreateAttemptAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(createDraw.execute(any()))
                .thenThrow(new DrawAccessDeniedException("only the group owner may perform this operation"));

        mockMvc.perform(post("/api/v1/draws")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void rejectsUnauthenticatedCreateRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/draws")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void getsDraw() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID drawId = UUID.randomUUID();
        when(getDraw.execute(eq(TENANT_ID), any())).thenReturn(result(drawId, "OPEN"));

        mockMvc.perform(get("/api/v1/draws/" + drawId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drawId").value(drawId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void reportsMissingDrawAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getDraw.execute(eq(TENANT_ID), any()))
                .thenThrow(new DrawNotFoundException("draw does not exist"));

        mockMvc.perform(get("/api/v1/draws/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("draw-not-found"));
    }

    @Test
    void rejectsUnauthenticatedGetRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/draws/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void listsDrawsWithDefaultPagination() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        DrawPageRequest expectedRequest = new DrawPageRequest(0, 20, DrawSortField.CREATED_AT, SortDirection.ASC);
        when(listDraws.execute(TENANT_ID, expectedRequest))
                .thenReturn(new DrawPage<>(List.of(summary(), summary()), 0, 20, 2));

        mockMvc.perform(get("/api/v1/draws"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void listsDrawsWithExplicitPaginationAndSort() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        DrawPageRequest expectedRequest = new DrawPageRequest(1, 2, DrawSortField.SCHEDULED_AT, SortDirection.DESC);
        when(listDraws.execute(TENANT_ID, expectedRequest))
                .thenReturn(new DrawPage<>(List.of(summary()), 1, 2, 3));

        mockMvc.perform(get("/api/v1/draws")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "scheduledAt")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.hasPrevious").value(true));
    }

    @Test
    void rejectsInvalidSortAndPaginationParameters() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(get("/api/v1/draws").param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/draws").param("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/draws").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/draws").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void rejectsUnauthenticatedListRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/draws"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void conductsDraw() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID drawId = UUID.randomUUID();
        when(conductDraw.execute(any())).thenReturn(result(drawId, "OPEN"));

        mockMvc.perform(patch("/api/v1/draws/" + drawId + "/conduct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void reportsInvalidConductTransitionAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(conductDraw.execute(any()))
                .thenThrow(new InvalidDrawStateException("draw cannot be opened"));

        mockMvc.perform(patch("/api/v1/draws/" + UUID.randomUUID() + "/conduct"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("draw-validation-failed"));
    }

    @Test
    void reportsNonOwnerConductAttemptAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(conductDraw.execute(any()))
                .thenThrow(new DrawAccessDeniedException("only the group owner may perform this operation"));

        mockMvc.perform(patch("/api/v1/draws/" + UUID.randomUUID() + "/conduct"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void reportsMissingDrawOnConductAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(conductDraw.execute(any()))
                .thenThrow(new DrawNotFoundException("draw does not exist"));

        mockMvc.perform(patch("/api/v1/draws/" + UUID.randomUUID() + "/conduct"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("draw-not-found"));
    }

    @Test
    void rejectsUnauthenticatedConductRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(patch("/api/v1/draws/" + UUID.randomUUID() + "/conduct"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void closesDrawWithWinner() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID drawId = UUID.randomUUID();
        when(closeDraw.execute(any())).thenReturn(result(drawId, "COMPLETED"));

        mockMvc.perform(patch("/api/v1/draws/" + drawId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void rejectsInvalidWinnerIdFormat() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(patch("/api/v1/draws/" + UUID.randomUUID() + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "not-a-uuid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void reportsNonOwnerCloseAttemptAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(closeDraw.execute(any()))
                .thenThrow(new DrawAccessDeniedException("only the group owner may perform this operation"));

        mockMvc.perform(patch("/api/v1/draws/" + UUID.randomUUID() + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void reportsInvalidCloseTransitionAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(closeDraw.execute(any()))
                .thenThrow(new InvalidDrawStateException("only an open draw can be completed"));

        mockMvc.perform(patch("/api/v1/draws/" + UUID.randomUUID() + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("draw-validation-failed"));
    }

    @Test
    void reportsMissingDrawOnCloseAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(closeDraw.execute(any()))
                .thenThrow(new DrawNotFoundException("draw does not exist"));

        mockMvc.perform(patch("/api/v1/draws/" + UUID.randomUUID() + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("draw-not-found"));
    }

    @Test
    void rejectsUnauthenticatedCloseRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(patch("/api/v1/draws/" + UUID.randomUUID() + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                TENANT_ID,
                Set.of("GROUP_MEMBER"),
                Set.of("draw.read"));
    }

    private DrawResult result(UUID drawId, String status) {
        Instant now = Instant.parse("2026-07-07T08:00:00Z");
        return new DrawResult(
                drawId,
                TENANT_ID.value(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "AUCTION",
                status,
                now.plusSeconds(3600),
                null,
                List.of(),
                now,
                now,
                0);
    }

    private DrawSummary summary() {
        return new DrawSummary(
                UUID.randomUUID(), 1, "RANDOM", "SCHEDULED", Instant.parse("2026-07-07T08:00:00Z"), null);
    }

    private String validRequestBody() {
        return """
                {
                  "groupId": "%s",
                  "cycleId": "%s",
                  "drawNumber": 1,
                  "type": "AUCTION",
                  "scheduledAt": "2026-08-01T10:00:00Z"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
