package in.bachatsetu.backend.notification.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.command.MarkNotificationDeliveredCommand;
import in.bachatsetu.backend.notification.application.command.MarkNotificationFailedCommand;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.query.NotificationSummary;
import in.bachatsetu.backend.notification.application.usecase.GetNotificationUseCase;
import in.bachatsetu.backend.notification.application.usecase.ListNotificationsUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.port.NotificationPage;
import in.bachatsetu.backend.notification.domain.port.NotificationPageRequest;
import in.bachatsetu.backend.notification.domain.port.NotificationSortField;
import in.bachatsetu.backend.notification.domain.port.SortDirection;
import in.bachatsetu.backend.notification.interfaces.rest.dto.CreateNotificationRequest;
import in.bachatsetu.backend.notification.interfaces.rest.dto.NotificationResponse;
import in.bachatsetu.backend.notification.interfaces.rest.dto.NotificationSummaryResponse;
import in.bachatsetu.backend.notification.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationApiMapperTest {

    private final NotificationApiMapper mapper = new NotificationApiMapper();

    @Test
    void mapsCreateRequestToCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID recipientUserId = UUID.randomUUID();
        CreateNotificationRequest request = new CreateNotificationRequest(
                recipientUserId.toString(), "member@example.com", "EMAIL", "VERIFICATION",
                Map.of("memberName", "Asha"));

        CreateNotificationCommand command = mapper.toCreateCommand(request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.recipientUserId().value()).isEqualTo(recipientUserId);
        assertThat(command.destination()).isEqualTo("member@example.com");
        assertThat(command.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void mapsInAppChannelToThePushDomainValue() {
        AuthenticatedUser currentUser = authenticatedUser();
        CreateNotificationRequest request = new CreateNotificationRequest(
                UUID.randomUUID().toString(), "device-token", "IN_APP", "GROUP_UPDATE", Map.of());

        CreateNotificationCommand command = mapper.toCreateCommand(request, currentUser);

        assertThat(command.channel()).isEqualTo(NotificationChannel.PUSH);
    }

    @Test
    void mapsThePushDomainValueBackToInAppInResponses() {
        NotificationResult result = result(UUID.randomUUID(), "SENT", NotificationChannel.PUSH.name());

        NotificationResponse response = mapper.toResponse(result);

        assertThat(response.channel()).isEqualTo("IN_APP");
    }

    @Test
    void getNotificationDelegatesToUseCaseWithParsedIdentifiers() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID notificationId = UUID.randomUUID();
        NotificationResult expected = result(notificationId, "SENT", "EMAIL");
        GetNotificationUseCase useCase = (tenantId, id) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(id.value()).isEqualTo(notificationId);
            return expected;
        };

        assertThat(mapper.getNotification(useCase, currentUser, notificationId.toString())).isEqualTo(expected);
    }

    @Test
    void mapsMarkDeliveredCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID notificationId = UUID.randomUUID();

        MarkNotificationDeliveredCommand command = mapper.toMarkDeliveredCommand(notificationId.toString(), currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.notificationId().value()).isEqualTo(notificationId);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void mapsMarkFailedCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID notificationId = UUID.randomUUID();

        MarkNotificationFailedCommand command =
                mapper.toMarkFailedCommand(notificationId.toString(), "provider-timeout", currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.notificationId().value()).isEqualTo(notificationId);
        assertThat(command.failureCode()).isEqualTo("provider-timeout");
    }

    @Test
    void buildsPageRequestFromValidatedRestParameters() {
        NotificationPageRequest pageRequest = mapper.toPageRequest(1, 10, "scheduledAt", "desc");

        assertThat(pageRequest.page()).isEqualTo(1);
        assertThat(pageRequest.size()).isEqualTo(10);
        assertThat(pageRequest.sortField()).isEqualTo(NotificationSortField.SCHEDULED_AT);
        assertThat(pageRequest.direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void rejectsUnsupportedSortOrDirectionValues() {
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "unsupported", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "scheduledAt", "sideways"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listNotificationsConsolidatesPageRequestAndResponseForTheController() {
        AuthenticatedUser currentUser = authenticatedUser();
        ListNotificationsUseCase useCase = (tenantId, request) -> new NotificationPage<>(List.of(summary()), 0, 20, 1);

        PageResponse<NotificationSummaryResponse> response =
                mapper.listNotifications(useCase, currentUser, 0, 20, "createdAt", "asc");

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void mapsNotificationPageToPageResponse() {
        NotificationPage<NotificationSummary> page = new NotificationPage<>(List.of(summary(), summary()), 0, 2, 3);

        PageResponse<NotificationSummaryResponse> response = mapper.toSummaryPage(page);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("notification.read"));
    }

    private NotificationResult result(UUID notificationId, String status, String channel) {
        Instant now = Instant.parse("2026-07-08T08:00:00Z");
        return new NotificationResult(
                notificationId, UUID.randomUUID(), UUID.randomUUID(), "member@example.com", channel,
                "VERIFICATION", "Account verification", "Please verify your account.", status,
                now, now, now, null, null, 0);
    }

    private NotificationSummary summary() {
        return new NotificationSummary(
                UUID.randomUUID(), "EMAIL", "VERIFICATION", "SENT", Instant.parse("2026-07-08T08:00:00Z"),
                Instant.parse("2026-07-08T08:00:00Z"));
    }
}
