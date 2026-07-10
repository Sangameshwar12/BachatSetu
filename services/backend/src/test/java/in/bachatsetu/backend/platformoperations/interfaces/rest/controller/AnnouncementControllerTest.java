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
import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.platformoperations.application.usecase.ListActiveAnnouncementsUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.ListAnnouncementsUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.PublishAnnouncementUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.AnnouncementSeverity;
import in.bachatsetu.backend.platformoperations.interfaces.rest.exception.PlatformOperationsExceptionHandler;
import in.bachatsetu.backend.platformoperations.interfaces.rest.mapper.PlatformOperationsApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import java.time.Instant;
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

@WebMvcTest(AnnouncementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PlatformOperationsApiMapper.class, PlatformOperationsExceptionHandler.class})
class AnnouncementControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublishAnnouncementUseCase publishAnnouncement;

    @MockBean
    private ListAnnouncementsUseCase listAnnouncements;

    @MockBean
    private ListActiveAnnouncementsUseCase listActiveAnnouncements;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void publishesAnAnnouncement() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(publishAnnouncement.execute(any())).thenReturn(announcementResult());

        mockMvc.perform(post("/api/v1/platform-operations/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Maintenance","message":"Downtime","startAt":"2026-07-10T08:00:00Z",
                                "endAt":"2026-07-11T08:00:00Z","severity":"WARNING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Maintenance"));
    }

    @Test
    void listsEveryAnnouncement() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(listAnnouncements.execute(any())).thenReturn(new Page<>(List.of(announcementResult()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/platform-operations/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listsActiveAnnouncements() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(listActiveAnnouncements.execute()).thenReturn(List.of(announcementResult()));

        mockMvc.perform(get("/api/v1/platform-operations/announcements/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    private AnnouncementResult announcementResult() {
        return new AnnouncementResult(
                AggregateId.newId(), "Maintenance", "Downtime", NOW, NOW.plusSeconds(3600),
                AnnouncementSeverity.WARNING, true);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
