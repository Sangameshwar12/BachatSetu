package in.bachatsetu.backend.platformoperations.interfaces.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.platformoperations.application.query.SystemHealthResult;
import in.bachatsetu.backend.platformoperations.application.usecase.GetSystemHealthUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;
import in.bachatsetu.backend.platformoperations.domain.model.HealthStatus;
import in.bachatsetu.backend.platformoperations.interfaces.rest.exception.PlatformOperationsExceptionHandler;
import in.bachatsetu.backend.platformoperations.interfaces.rest.mapper.PlatformOperationsApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemHealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PlatformOperationsApiMapper.class, PlatformOperationsExceptionHandler.class})
class SystemHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetSystemHealthUseCase getSystemHealth;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void returnsSystemHealth() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getSystemHealth.execute()).thenReturn(new SystemHealthResult(
                new ComponentHealth("database", HealthStatus.UP, "ok"),
                new ComponentHealth("storage", HealthStatus.UP, "ok"),
                new ComponentHealth("notification", HealthStatus.UP, "ok"), 60, "21.0.9", "1.0.0", null, 1, 2, 3, 4,
                5));

        mockMvc.perform(get("/api/v1/platform-operations/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database.status").value("UP"))
                .andExpect(jsonPath("$.javaVersion").value("21.0.9"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
