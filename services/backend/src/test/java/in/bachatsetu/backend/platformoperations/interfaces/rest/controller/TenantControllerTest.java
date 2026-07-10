package in.bachatsetu.backend.platformoperations.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.platformoperations.application.exception.PlatformOperationsApplicationException;
import in.bachatsetu.backend.platformoperations.application.exception.PlatformOperationsFailureReason;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.application.usecase.ActivateTenantUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.ArchiveTenantUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.GetTenantUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.SearchTenantsUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.SuspendTenantUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import in.bachatsetu.backend.platformoperations.interfaces.rest.exception.PlatformOperationsExceptionHandler;
import in.bachatsetu.backend.platformoperations.interfaces.rest.mapper.PlatformOperationsApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TenantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PlatformOperationsApiMapper.class, PlatformOperationsExceptionHandler.class})
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchTenantsUseCase searchTenants;

    @MockBean
    private GetTenantUseCase getTenant;

    @MockBean
    private SuspendTenantUseCase suspendTenant;

    @MockBean
    private ActivateTenantUseCase activateTenant;

    @MockBean
    private ArchiveTenantUseCase archiveTenant;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void searchesTenants() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(searchTenants.execute(any(), any())).thenReturn(new Page<>(List.of(tenantResult()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/platform-operations/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getsATenant() throws Exception {
        AggregateId tenantId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getTenant.execute(tenantId)).thenReturn(tenantResult());

        mockMvc.perform(get("/api/v1/platform-operations/tenants/{tenantId}", tenantId.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void suspendsATenant() throws Exception {
        AggregateId tenantId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(suspendTenant.execute(any())).thenReturn(tenantResult());

        mockMvc.perform(post("/api/v1/platform-operations/tenants/{tenantId}/suspend", tenantId.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Fraud\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void activatesATenant() throws Exception {
        AggregateId tenantId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(activateTenant.execute(any())).thenReturn(tenantResult());

        mockMvc.perform(post("/api/v1/platform-operations/tenants/{tenantId}/activate", tenantId.value()))
                .andExpect(status().isOk());
    }

    @Test
    void archivesATenant() throws Exception {
        AggregateId tenantId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(archiveTenant.execute(any())).thenReturn(tenantResult());

        mockMvc.perform(post("/api/v1/platform-operations/tenants/{tenantId}/archive", tenantId.value()))
                .andExpect(status().isOk());
    }

    @Test
    void mapsTenantNotFound() throws Exception {
        AggregateId tenantId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(activateTenant.execute(any())).thenThrow(new PlatformOperationsApplicationException(
                PlatformOperationsFailureReason.TENANT_NOT_FOUND, "no tenant"));

        mockMvc.perform(post("/api/v1/platform-operations/tenants/{tenantId}/activate", tenantId.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("tenant-not-found"));
    }

    private TenantResult tenantResult() {
        return new TenantResult(
                AggregateId.newId(), TenantStatus.ACTIVE, null, new TenantStatistics(1, 1, 1, 0, 0, 0, 1, null));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
