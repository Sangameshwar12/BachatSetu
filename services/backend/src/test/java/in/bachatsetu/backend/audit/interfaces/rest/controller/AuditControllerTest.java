package in.bachatsetu.backend.audit.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.audit.application.exception.AuditEntryNotFoundException;
import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.application.usecase.GetAuditEntryUseCase;
import in.bachatsetu.backend.audit.application.usecase.SearchAuditUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.audit.interfaces.rest.exception.AuditExceptionHandler;
import in.bachatsetu.backend.audit.interfaces.rest.mapper.AuditApiMapper;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AuditApiMapper.class, AuditExceptionHandler.class})
class AuditControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateAuditEntryUseCase createAuditEntry;

    @MockBean
    private GetAuditEntryUseCase getAuditEntry;

    @MockBean
    private SearchAuditUseCase searchAudit;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsAnAuditEntry() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID auditId = UUID.randomUUID();
        when(createAuditEntry.execute(any())).thenReturn(newResult(auditId));

        mockMvc.perform(post("/api/v1/audit")
                        .contentType("application/json")
                        .content("""
                                {"eventType":"LOGIN","moduleName":"auth","action":"LOGIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auditId").value(auditId.toString()))
                .andExpect(jsonPath("$.eventType").value("LOGIN"));
    }

    @Test
    void rejectsUnauthenticatedCreate() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/audit")
                        .contentType("application/json")
                        .content("""
                                {"eventType":"LOGIN","moduleName":"auth","action":"LOGIN"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void rejectsABlankModuleName() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/audit")
                        .contentType("application/json")
                        .content("""
                                {"eventType":"LOGIN","moduleName":" ","action":"LOGIN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void getsAnAuditEntryById() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID auditId = UUID.randomUUID();
        when(getAuditEntry.execute(any(), any())).thenReturn(newResult(auditId));

        mockMvc.perform(get("/api/v1/audit/" + auditId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditId").value(auditId.toString()));
    }

    @Test
    void reportsAnUnknownEntryAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getAuditEntry.execute(any(), any())).thenThrow(new AuditEntryNotFoundException("not found"));

        mockMvc.perform(get("/api/v1/audit/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    void searchesAuditEntries() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        AuditEntryResult result = newResult(UUID.randomUUID());
        when(searchAudit.execute(any())).thenReturn(new AuditPage<>(List.of(result), 0, 20, 1));

        mockMvc.perform(get("/api/v1/audit").param("module", "auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].auditId").value(result.auditId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rejectsAnInvalidSortDirection() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(get("/api/v1/audit").param("direction", "sideways"))
                .andExpect(status().isBadRequest());
    }

    private AuditEntryResult newResult(UUID auditId) {
        return new AuditEntryResult(
                auditId, UUID.randomUUID(), UUID.randomUUID(), AuditEventType.LOGIN, "auth", null, null, "LOGIN",
                null, null, null, null, NOW);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of("GROUP_MEMBER"),
                Set.of("audit.write"));
    }
}
