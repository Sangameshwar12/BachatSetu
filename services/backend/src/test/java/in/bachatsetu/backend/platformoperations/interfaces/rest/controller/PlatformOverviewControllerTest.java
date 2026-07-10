package in.bachatsetu.backend.platformoperations.interfaces.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.platformoperations.application.query.PlatformOverviewResult;
import in.bachatsetu.backend.platformoperations.application.usecase.GetPlatformOverviewUseCase;
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

@WebMvcTest(PlatformOverviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PlatformOperationsApiMapper.class, PlatformOperationsExceptionHandler.class})
class PlatformOverviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetPlatformOverviewUseCase getPlatformOverview;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void returnsThePlatformOverview() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getPlatformOverview.execute()).thenReturn(
                new PlatformOverviewResult(10, 2, 3, 4, 5, 6, 7, 8, 9, 100_000, 1, 2, 3, 4, 5));

        mockMvc.perform(get("/api/v1/platform-operations/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.totalRevenuePaise").value(100_000));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
