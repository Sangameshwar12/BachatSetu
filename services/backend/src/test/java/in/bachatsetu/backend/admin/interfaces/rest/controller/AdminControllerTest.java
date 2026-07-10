package in.bachatsetu.backend.admin.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.admin.application.exception.PlatformUserNotFoundException;
import in.bachatsetu.backend.admin.application.query.PlatformGroupResult;
import in.bachatsetu.backend.admin.application.query.PlatformStatisticsResult;
import in.bachatsetu.backend.admin.application.query.PlatformTenantResult;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.application.usecase.DisableUserUseCase;
import in.bachatsetu.backend.admin.application.usecase.EnableUserUseCase;
import in.bachatsetu.backend.admin.application.usecase.GetPlatformStatisticsUseCase;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformGroupsUseCase;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformTenantsUseCase;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformUsersUseCase;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.interfaces.rest.config.AdminProperties;
import in.bachatsetu.backend.admin.interfaces.rest.exception.AdminExceptionHandler;
import in.bachatsetu.backend.admin.interfaces.rest.mapper.AdminApiMapper;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AdminApiMapper.class, AdminExceptionHandler.class, AdminControllerTest.MethodSecurityTestConfig.class})
class AdminControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetPlatformStatisticsUseCase getPlatformStatistics;

    @MockBean
    private ListPlatformUsersUseCase listPlatformUsers;

    @MockBean
    private ListPlatformGroupsUseCase listPlatformGroups;

    @MockBean
    private ListPlatformTenantsUseCase listPlatformTenants;

    @MockBean
    private EnableUserUseCase enableUser;

    @MockBean
    private DisableUserUseCase disableUser;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @MockBean
    private AdminProperties adminProperties;

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanReadStatistics() throws Exception {
        when(adminProperties.pageSizeDefault()).thenReturn(20);
        when(adminProperties.pageSizeMax()).thenReturn(100);
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getPlatformStatistics.execute()).thenReturn(
                new PlatformStatisticsResult(10, 8, 2, 5, 4, 20, 15, 15, 30, 7));

        mockMvc.perform(get("/api/v1/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.completedPayments").value(15));
    }

    @Test
    @WithMockUser(authorities = "GROUP_MEMBER")
    void aNonAdministratorIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/statistics"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("platform-administrator-required"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void listsUsersWithFilters() throws Exception {
        when(adminProperties.pageSizeDefault()).thenReturn(20);
        when(adminProperties.pageSizeMax()).thenReturn(100);
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        PlatformUserResult result = new PlatformUserResult(
                UUID.randomUUID(), UUID.randomUUID(), "a@b.com", null, "Asha", "Rao", PlatformUserStatus.ACTIVE, NOW);
        when(listPlatformUsers.execute(any())).thenReturn(new PlatformPage<>(List.of(result), 0, 20, 1));

        mockMvc.perform(get("/api/v1/admin/users").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("a@b.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void listsGroups() throws Exception {
        when(adminProperties.pageSizeDefault()).thenReturn(20);
        when(adminProperties.pageSizeMax()).thenReturn(100);
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        PlatformGroupResult result = new PlatformGroupResult(
                UUID.randomUUID(), UUID.randomUUID(), "GRP-1", "name", PlatformGroupStatus.ACTIVE, 4, NOW);
        when(listPlatformGroups.execute(any())).thenReturn(new PlatformPage<>(List.of(result), 0, 20, 1));

        mockMvc.perform(get("/api/v1/admin/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("GRP-1"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void listsTenants() throws Exception {
        when(adminProperties.pageSizeDefault()).thenReturn(20);
        when(adminProperties.pageSizeMax()).thenReturn(100);
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        PlatformTenantResult result = new PlatformTenantResult(UUID.randomUUID(), 5, 2);
        when(listPlatformTenants.execute(any())).thenReturn(new PlatformPage<>(List.of(result), 0, 20, 1));

        mockMvc.perform(get("/api/v1/admin/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userCount").value(5));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void enablesAUser() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID userId = UUID.randomUUID();
        when(enableUser.execute(any())).thenReturn(new PlatformUserResult(
                userId, UUID.randomUUID(), null, null, null, null, PlatformUserStatus.ACTIVE, NOW));

        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void disablesAUser() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID userId = UUID.randomUUID();
        when(disableUser.execute(any())).thenReturn(new PlatformUserResult(
                userId, UUID.randomUUID(), null, null, null, null, PlatformUserStatus.DISABLED, NOW));

        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void reportsAnUnknownUserAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(enableUser.execute(any())).thenThrow(new PlatformUserNotFoundException("not found"));

        mockMvc.perform(post("/api/v1/admin/users/" + UUID.randomUUID() + "/enable"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void rejectsAnUnauthenticatedRequestOnceInsideTheController() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/admin/statistics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of("PLATFORM_ADMIN"),
                Set.of());
    }

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityTestConfig {
    }
}
