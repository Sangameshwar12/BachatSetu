package in.bachatsetu.backend.admin.interfaces.rest.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/** Notification analytics. */
public record NotificationAnalyticsResponse(

        @Schema(description = "Total number of notifications") long totalNotifications,
        @Schema(description = "Number of notifications not yet delivered") long unreadNotifications,
        @Schema(description = "Delivery status distribution") List<DistributionEntryResponse> deliveryStatusCounts,
        @Schema(description = "Notification type distribution")
        List<DistributionEntryResponse> notificationTypeDistribution) {

    public NotificationAnalyticsResponse {
        deliveryStatusCounts =
                List.copyOf(Objects.requireNonNull(deliveryStatusCounts, "deliveryStatusCounts must not be null"));
        notificationTypeDistribution = List.copyOf(Objects.requireNonNull(
                notificationTypeDistribution, "notificationTypeDistribution must not be null"));
    }
}
