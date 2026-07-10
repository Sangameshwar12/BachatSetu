package in.bachatsetu.backend.dashboard.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.dashboard.application.exception.NoActiveGroupException;
import in.bachatsetu.backend.dashboard.application.query.CurrentGroupSummary;
import in.bachatsetu.backend.dashboard.application.query.MemberDashboardResult;
import in.bachatsetu.backend.dashboard.application.query.OrganizerDashboardResult;
import in.bachatsetu.backend.dashboard.application.usecase.GetMemberDashboardUseCase;
import in.bachatsetu.backend.dashboard.application.usecase.GetOrganizerDashboardUseCase;
import in.bachatsetu.backend.dashboard.interfaces.rest.exception.DashboardExceptionHandler;
import in.bachatsetu.backend.dashboard.interfaces.rest.mapper.DashboardApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({DashboardApiMapper.class, DashboardExceptionHandler.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetMemberDashboardUseCase getMemberDashboard;

    @MockBean
    private GetOrganizerDashboardUseCase getOrganizerDashboard;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void returnsTheMemberDashboard() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        CurrentGroupSummary currentGroup = new CurrentGroupSummary(
                AggregateId.newId(), "BS-TEST", "Bachat Circle", 100_000L, "INR", "MONTHLY", 5, 10);
        when(getMemberDashboard.execute(any(), any()))
                .thenReturn(new MemberDashboardResult(currentGroup, null, null, List.of()));

        mockMvc.perform(get("/api/v1/dashboard/member"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGroup.groupCode").value("BS-TEST"));
    }

    @Test
    void mapsNoActiveGroupToNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getMemberDashboard.execute(any(), any()))
                .thenThrow(new NoActiveGroupException("no active group"));

        mockMvc.perform(get("/api/v1/dashboard/member"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("no-active-group"));
    }

    @Test
    void returnsTheOrganizerDashboard() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getOrganizerDashboard.execute(any(), any()))
                .thenReturn(new OrganizerDashboardResult(List.of(), List.of()));

        mockMvc.perform(get("/api/v1/dashboard/organizer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups").isEmpty());
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
