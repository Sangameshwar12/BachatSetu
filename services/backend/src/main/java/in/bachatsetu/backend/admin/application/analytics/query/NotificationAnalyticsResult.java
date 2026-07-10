package in.bachatsetu.backend.admin.application.analytics.query;

import in.bachatsetu.backend.admin.domain.analytics.model.DistributionEntry;
import java.util.List;
import java.util.Objects;

/** Application-layer read model mirroring {@code NotificationAnalytics}. */
public record NotificationAnalyticsResult(
        long totalNotifications,
        long unreadNotifications,
        List<DistributionEntry> deliveryStatusCounts,
        List<DistributionEntry> notificationTypeDistribution) {

    public NotificationAnalyticsResult {
        deliveryStatusCounts =
                List.copyOf(Objects.requireNonNull(deliveryStatusCounts, "deliveryStatusCounts must not be null"));
        notificationTypeDistribution = List.copyOf(Objects.requireNonNull(
                notificationTypeDistribution, "notificationTypeDistribution must not be null"));
    }
}
