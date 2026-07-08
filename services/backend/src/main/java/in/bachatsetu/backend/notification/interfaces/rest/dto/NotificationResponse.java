package in.bachatsetu.backend.notification.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Safe presentation view of a notification returned by the REST API. */
public record NotificationResponse(

        @Schema(description = "Notification identifier") String notificationId,
        @Schema(description = "Owning tenant identifier") String tenantId,
        @Schema(description = "Identifier of the recipient user") String recipientUserId,
        @Schema(description = "Channel-specific destination") String destination,
        @Schema(description = "Delivery channel", example = "EMAIL") String channel,
        @Schema(description = "Notification category", example = "PAYMENT_RECEIPT") String category,
        @Schema(description = "Rendered subject, if the channel supports one") String subject,
        @Schema(description = "Rendered message body") String body,
        @Schema(description = "Lifecycle status", example = "SENT") String status,
        @Schema(description = "Timestamp the notification was scheduled for") Instant scheduledAt,
        @Schema(description = "Timestamp the notification was created") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Timestamp the notification was confirmed delivered, if applicable")
                Instant deliveredAt,
        @Schema(description = "Failure reason, once failed") String failureReason,
        @Schema(description = "Optimistic-lock version") long version) {
}
