package in.bachatsetu.backend.admin.domain.analytics.model;

import java.util.List;
import java.util.Objects;

/**
 * Notification analytics. {@code unreadNotifications} approximates "unread" as "not yet delivered" — the
 * Notification module tracks only delivery lifecycle, not a recipient read receipt; see
 * {@code docs/application/admin-analytics.md} for the full rationale.
 */
public record NotificationAnalytics(
        long totalNotifications,
        long unreadNotifications,
        List<DistributionEntry> deliveryStatusCounts,
        List<DistributionEntry> notificationTypeDistribution) {

    public NotificationAnalytics {
        if (totalNotifications < 0) {
            throw new IllegalArgumentException("totalNotifications must not be negative");
        }
        if (unreadNotifications < 0) {
            throw new IllegalArgumentException("unreadNotifications must not be negative");
        }
        deliveryStatusCounts =
                List.copyOf(Objects.requireNonNull(deliveryStatusCounts, "deliveryStatusCounts must not be null"));
        notificationTypeDistribution = List.copyOf(Objects.requireNonNull(
                notificationTypeDistribution, "notificationTypeDistribution must not be null"));
    }
}
