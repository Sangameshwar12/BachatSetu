package in.bachatsetu.backend.notification.interfaces.rest.controller;

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
import in.bachatsetu.backend.notification.application.exception.NotificationNotFoundException;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.query.NotificationSummary;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.application.usecase.GetNotificationUseCase;
import in.bachatsetu.backend.notification.application.usecase.ListNotificationsUseCase;
import in.bachatsetu.backend.notification.application.usecase.MarkNotificationDeliveredUseCase;
import in.bachatsetu.backend.notification.application.usecase.MarkNotificationFailedUseCase;
import in.bachatsetu.backend.notification.domain.port.NotificationPage;
import in.bachatsetu.backend.notification.domain.port.NotificationPageRequest;
import in.bachatsetu.backend.notification.domain.port.NotificationSortField;
import in.bachatsetu.backend.notification.domain.port.SortDirection;
import in.bachatsetu.backend.notification.interfaces.rest.exception.NotificationExceptionHandler;
import in.bachatsetu.backend.notification.interfaces.rest.mapper.NotificationApiMapper;
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

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({NotificationApiMapper.class, NotificationExceptionHandler.class})
class NotificationControllerTest {

    private static final AggregateId TENANT_ID = AggregateId.newId();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateNotificationUseCase createNotification;

    @MockBean
    private GetNotificationUseCase getNotification;

    @MockBean
    private ListNotificationsUseCase listNotifications;

    @MockBean
    private MarkNotificationDeliveredUseCase markNotificationDelivered;

    @MockBean
    private MarkNotificationFailedUseCase markNotificationFailed;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsNotification() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID notificationId = UUID.randomUUID();
        when(createNotification.execute(any())).thenReturn(result(notificationId, "SENT"));

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/notifications/" + notificationId))
                .andExpect(jsonPath("$.notificationId").value(notificationId.toString()))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void rejectsInvalidChannelValue() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientUserId": "%s", "destination": "member@example.com",
                                 "channel": "CARRIER_PIGEON", "category": "VERIFICATION"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void rejectsUnauthenticatedCreateRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void getsNotification() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID notificationId = UUID.randomUUID();
        when(getNotification.execute(eq(TENANT_ID), any())).thenReturn(result(notificationId, "SENT"));

        mockMvc.perform(get("/api/v1/notifications/" + notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(notificationId.toString()));
    }

    @Test
    void reportsMissingNotificationAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getNotification.execute(eq(TENANT_ID), any()))
                .thenThrow(new NotificationNotFoundException("notification does not exist"));

        mockMvc.perform(get("/api/v1/notifications/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("notification-not-found"));
    }

    @Test
    void listsNotificationsWithDefaultPagination() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        NotificationPageRequest expectedRequest =
                new NotificationPageRequest(0, 20, NotificationSortField.CREATED_AT, SortDirection.ASC);
        when(listNotifications.execute(TENANT_ID, expectedRequest))
                .thenReturn(new NotificationPage<>(List.of(summary(), summary()), 0, 20, 2));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void rejectsInvalidSortAndPaginationParameters() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(get("/api/v1/notifications").param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/notifications").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void marksANotificationDelivered() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID notificationId = UUID.randomUUID();
        when(markNotificationDelivered.execute(any())).thenReturn(result(notificationId, "DELIVERED"));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/delivered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void marksANotificationFailed() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID notificationId = UUID.randomUUID();
        when(markNotificationFailed.execute(any())).thenReturn(result(notificationId, "FAILED"));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/failed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"failureCode": "provider-timeout"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void rejectsMarkFailedWithoutAFailureCode() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(patch("/api/v1/notifications/" + UUID.randomUUID() + "/failed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void reportsMissingNotificationOnMarkDeliveredAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(markNotificationDelivered.execute(any()))
                .thenThrow(new NotificationNotFoundException("notification does not exist"));

        mockMvc.perform(patch("/api/v1/notifications/" + UUID.randomUUID() + "/delivered"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("notification-not-found"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                TENANT_ID,
                Set.of("GROUP_MEMBER"),
                Set.of("notification.read"));
    }

    private NotificationResult result(UUID notificationId, String status) {
        Instant now = Instant.parse("2026-07-08T08:00:00Z");
        return new NotificationResult(
                notificationId, TENANT_ID.value(), UUID.randomUUID(), "member@example.com", "EMAIL",
                "VERIFICATION", "Account verification", "Please verify your account.", status,
                now, now, now, null, null, 0);
    }

    private NotificationSummary summary() {
        return new NotificationSummary(
                UUID.randomUUID(), "EMAIL", "VERIFICATION", "SENT", Instant.parse("2026-07-08T08:00:00Z"),
                Instant.parse("2026-07-08T08:00:00Z"));
    }

    private String validRequestBody() {
        return """
                {
                  "recipientUserId": "%s",
                  "destination": "member@example.com",
                  "channel": "EMAIL",
                  "category": "VERIFICATION",
                  "placeholders": {"memberName": "Asha"}
                }
                """.formatted(UUID.randomUUID());
    }
}
