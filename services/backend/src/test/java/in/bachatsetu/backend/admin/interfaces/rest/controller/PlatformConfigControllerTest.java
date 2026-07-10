package in.bachatsetu.backend.admin.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.admin.application.configuration.query.FeatureFlagResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetConfigurationUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetFeatureFlagsUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetSystemLimitsUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateConfigurationUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateFeatureFlagsUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateSystemLimitsUseCase;
import in.bachatsetu.backend.admin.interfaces.rest.exception.AdminExceptionHandler;
import in.bachatsetu.backend.admin.interfaces.rest.mapper.PlatformConfigApiMapper;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlatformConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    PlatformConfigApiMapper.class, AdminExceptionHandler.class,
    PlatformConfigControllerTest.MethodSecurityTestConfig.class
})
class PlatformConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetConfigurationUseCase getConfiguration;

    @MockBean
    private UpdateConfigurationUseCase updateConfiguration;

    @MockBean
    private GetFeatureFlagsUseCase getFeatureFlags;

    @MockBean
    private UpdateFeatureFlagsUseCase updateFeatureFlags;

    @MockBean
    private GetSystemLimitsUseCase getSystemLimits;

    @MockBean
    private UpdateSystemLimitsUseCase updateSystemLimits;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanReadConfiguration() throws Exception {
        when(getConfiguration.execute()).thenReturn(new PlatformConfigurationResult(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, false, null, null, null, 0,
                Instant.parse("2026-07-09T08:00:00Z"), null));

        mockMvc.perform(get("/api/v1/admin/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLanguage").value("ENGLISH"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanUpdateConfiguration() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(updateConfiguration.execute(any())).thenReturn(new PlatformConfigurationResult(
                "HINDI", 600, "AWS_S3", "STRIPE", 5, 20_000_000L, 200, 40, false, null, null, null, 1,
                Instant.parse("2026-07-09T08:00:00Z"), null));

        mockMvc.perform(put("/api/v1/admin/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "defaultLanguage": "HINDI",
                                  "otpExpirySeconds": 600,
                                  "defaultStorageProvider": "AWS_S3",
                                  "defaultPaymentProvider": "STRIPE",
                                  "notificationRetryCount": 5,
                                  "maximumUploadSizeBytes": 20000000,
                                  "maximumMembersPerGroup": 200,
                                  "maximumGroupsPerOrganizer": 40,
                                  "maintenanceEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLanguage").value("HINDI"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanReadFeatureFlags() throws Exception {
        when(getFeatureFlags.execute()).thenReturn(
                List.of(new FeatureFlagResult("PAYMENTS", true, 0, Instant.parse("2026-07-09T08:00:00Z"), null)));

        mockMvc.perform(get("/api/v1/admin/config/feature-flags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("PAYMENTS"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanUpdateFeatureFlags() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(updateFeatureFlags.execute(any())).thenReturn(
                List.of(new FeatureFlagResult("PAYMENTS", false, 1, Instant.parse("2026-07-09T08:00:00Z"), null)));

        mockMvc.perform(put("/api/v1/admin/config/feature-flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flags\":{\"PAYMENTS\":false}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enabled").value(false));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanReadSystemLimits() throws Exception {
        when(getSystemLimits.execute()).thenReturn(
                List.of(new PlatformLimitResult("MAX_GROUPS", 500, 0, Instant.parse("2026-07-09T08:00:00Z"), null)));

        mockMvc.perform(get("/api/v1/admin/config/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("MAX_GROUPS"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_PLATFORM_ADMIN")
    void platformAdministratorCanUpdateSystemLimits() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(updateSystemLimits.execute(any())).thenReturn(
                List.of(new PlatformLimitResult("MAX_GROUPS", 600, 1, Instant.parse("2026-07-09T08:00:00Z"), null)));

        mockMvc.perform(put("/api/v1/admin/config/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limits\":{\"MAX_GROUPS\":600}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(600));
    }

    @Test
    @WithMockUser(authorities = "GROUP_MEMBER")
    void aNonAdministratorIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/config"))
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
