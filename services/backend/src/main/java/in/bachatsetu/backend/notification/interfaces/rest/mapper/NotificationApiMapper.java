package in.bachatsetu.backend.notification.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.command.MarkNotificationDeliveredCommand;
import in.bachatsetu.backend.notification.application.command.MarkNotificationFailedCommand;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.query.NotificationSummary;
import in.bachatsetu.backend.notification.application.usecase.GetNotificationUseCase;
import in.bachatsetu.backend.notification.application.usecase.ListNotificationsUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
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
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Notification application commands and safe responses. */
@Component
public class NotificationApiMapper {

    public CreateNotificationCommand toCreateCommand(CreateNotificationRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new CreateNotificationCommand(
                currentUser.tenantId(),
                AggregateId.from(request.recipientUserId()),
                request.destination(),
                toChannel(request.channel()),
                NotificationCategory.valueOf(request.category()),
                request.placeholders(),
                currentUser.userId().toAggregateId());
    }

    public NotificationResult getNotification(
            GetNotificationUseCase useCase, AuthenticatedUser currentUser, String notificationId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(notificationId, "notification id must not be null");
        return useCase.execute(currentUser.tenantId(), AggregateId.from(notificationId));
    }

    public MarkNotificationDeliveredCommand toMarkDeliveredCommand(String notificationId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(notificationId, "notification id must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new MarkNotificationDeliveredCommand(
                currentUser.tenantId(),
                AggregateId.from(notificationId),
                currentUser.userId().toAggregateId());
    }

    public MarkNotificationFailedCommand toMarkFailedCommand(
            String notificationId, String failureCode, AuthenticatedUser currentUser) {
        Objects.requireNonNull(notificationId, "notification id must not be null");
        Objects.requireNonNull(failureCode, "failure code must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new MarkNotificationFailedCommand(
                currentUser.tenantId(),
                AggregateId.from(notificationId),
                failureCode,
                currentUser.userId().toAggregateId());
    }

    public NotificationResponse toResponse(NotificationResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new NotificationResponse(
                result.notificationId().toString(),
                result.tenantId().toString(),
                result.recipientUserId().toString(),
                result.destination(),
                toChannelName(result.channel()),
                result.category(),
                result.subject(),
                result.body(),
                result.status(),
                result.scheduledAt(),
                result.createdAt(),
                result.updatedAt(),
                result.deliveredAt(),
                result.failureReason(),
                result.version());
    }

    public NotificationPageRequest toPageRequest(int page, int size, String sort, String direction) {
        return new NotificationPageRequest(page, size, toSortField(sort), toSortDirection(direction));
    }

    public NotificationPage<NotificationSummary> listNotifications(
            ListNotificationsUseCase useCase,
            AuthenticatedUser currentUser,
            NotificationPageRequest pageRequest) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return useCase.execute(currentUser.tenantId(), pageRequest);
    }

    public PageResponse<NotificationSummaryResponse> listNotifications(
            ListNotificationsUseCase useCase,
            AuthenticatedUser currentUser,
            int page,
            int size,
            String sort,
            String direction) {
        NotificationPageRequest pageRequest = toPageRequest(page, size, sort, direction);
        return toSummaryPage(listNotifications(useCase, currentUser, pageRequest));
    }

    public NotificationSummaryResponse toSummaryResponse(NotificationSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        return new NotificationSummaryResponse(
                summary.notificationId().toString(),
                toChannelName(summary.channel()),
                summary.category(),
                summary.status(),
                summary.scheduledAt(),
                summary.createdAt());
    }

    public PageResponse<NotificationSummaryResponse> toSummaryPage(NotificationPage<NotificationSummary> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<NotificationSummaryResponse> content = page.content().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(),
                page.hasNext(), page.hasPrevious());
    }

    /**
     * Translates the REST vocabulary's {@code IN_APP} to the pre-existing
     * {@link NotificationChannel#PUSH} domain value; see {@code InAppNotificationSender}'s Javadoc for why the
     * two names diverge.
     */
    private NotificationChannel toChannel(String channel) {
        return "IN_APP".equals(channel) ? NotificationChannel.PUSH : NotificationChannel.valueOf(channel);
    }

    private String toChannelName(String channel) {
        return NotificationChannel.PUSH.name().equals(channel) ? "IN_APP" : channel;
    }

    private NotificationSortField toSortField(String sort) {
        return switch (sort) {
            case "scheduledAt" -> NotificationSortField.SCHEDULED_AT;
            case "createdAt" -> NotificationSortField.CREATED_AT;
            default -> throw new IllegalArgumentException("unsupported sort field: " + sort);
        };
    }

    private SortDirection toSortDirection(String direction) {
        return switch (direction) {
            case "asc" -> SortDirection.ASC;
            case "desc" -> SortDirection.DESC;
            default -> throw new IllegalArgumentException("unsupported sort direction: " + direction);
        };
    }
}
