package in.bachatsetu.backend.admin.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.admin.application.analytics.query.GroupAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.NotificationAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.OverviewAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.PaymentAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.StorageAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.UserAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetGroupAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetNotificationAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetOverviewAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetPaymentAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetStorageAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetUserAnalyticsUseCase;
import in.bachatsetu.backend.admin.interfaces.rest.exception.AdminExceptionHandler;
import in.bachatsetu.backend.admin.interfaces.rest.mapper.AnalyticsApiMapper;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Set;
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

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AnalyticsApiMapper.class, AdminExceptionHandler.class, AnalyticsControllerTest.MethodSecurityTestConfig.class})
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetOverviewAnalyticsUseCase getOverviewAnalytics;

    @MockBean
    private GetPaymentAnalyticsUseCase getPaymentAnalytics;

    @MockBean
    private GetGroupAnalyticsUseCase getGroupAnalytics;

    @MockBean
    private GetUserAnalyticsUseCase getUserAnalytics;

    @MockBean
    private GetNotificationAnalyticsUseCase getNotificationAnalytics;

    @MockBean
    private GetStorageAnalyticsUseCase getStorageAnalytics;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanReadOverviewAnalytics() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getOverviewAnalytics.execute(any()))
                .thenReturn(new OverviewAnalyticsResult(10, 8, 2, 3, 5, 4, 1, 20, 15, 2, 15, 30, 7));

        mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanReadPaymentAnalytics() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getPaymentAnalytics.execute(any()))
                .thenReturn(new PaymentAnalyticsResult(100_000L, 80_000L, 2, 1, 5_000.0, 0.8, 0.2, List.of()));

        mockMvc.perform(get("/api/v1/admin/analytics/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaymentVolumePaise").value(100_000));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanReadGroupAnalytics() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getGroupAnalytics.execute(any()))
                .thenReturn(new GroupAnalyticsResult(10, 8, 2, 5.5, 250_000.0, List.of(), 0.75));

        mockMvc.perform(get("/api/v1/admin/analytics/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageMembersPerGroup").value(5.5));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanReadUserAnalytics() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getUserAnalytics.execute(any()))
                .thenReturn(new UserAnalyticsResult(10, 8, 2, List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/admin/analytics/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanReadNotificationAnalytics() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getNotificationAnalytics.execute(any()))
                .thenReturn(new NotificationAnalyticsResult(10, 5, List.of(), List.of()));

        mockMvc.perform(get("/api/v1/admin/analytics/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadNotifications").value(5));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanReadStorageAnalytics() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getStorageAnalytics.execute(any()))
                .thenReturn(new StorageAnalyticsResult(4, 4096, 1024.0, List.of(), List.of()));

        mockMvc.perform(get("/api/v1/admin/analytics/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStorageBytes").value(4096));
    }

    @Test
    @WithMockUser(authorities = "GROUP_MEMBER")
    void aNonAdministratorIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("platform-administrator-required"));
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
