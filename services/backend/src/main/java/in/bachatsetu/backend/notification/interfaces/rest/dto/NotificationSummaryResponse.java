package in.bachatsetu.backend.notification.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Compact presentation view of a notification returned by list endpoints. */
public record NotificationSummaryResponse(

        @Schema(description = "Notification identifier") String notificationId,
        @Schema(description = "Delivery channel", example = "EMAIL") String channel,
        @Schema(description = "Notification category", example = "PAYMENT_RECEIPT") String category,
        @Schema(description = "Lifecycle status", example = "SENT") String status,
        @Schema(description = "Timestamp the notification was scheduled for") Instant scheduledAt,
        @Schema(description = "Timestamp the notification was created") Instant createdAt) {
}
