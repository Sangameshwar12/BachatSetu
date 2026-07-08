package in.bachatsetu.backend.notification.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationApplicationMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private final NotificationApplicationMapper mapper = new NotificationApplicationMapper();

    @Test
    void mapsAQueuedNotificationToResultWithNoDeliveredAtOrFailureReason() {
        Notification notification = newNotification();

        var result = mapper.toResult(notification);

        assertThat(result.notificationId()).isEqualTo(notification.id().value());
        assertThat(result.channel()).isEqualTo("EMAIL");
        assertThat(result.category()).isEqualTo("VERIFICATION");
        assertThat(result.status()).isEqualTo("QUEUED");
        assertThat(result.deliveredAt()).isNull();
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void mapsADeliveredNotificationWithADeliveredAtTimestamp() {
        Notification notification = newNotification();
        notification.startDelivery(AggregateId.newId(), NOW);
        notification.markSent("provider-msg-1", AggregateId.newId(), NOW.plusSeconds(1));
        notification.markDelivered(AggregateId.newId(), NOW.plusSeconds(2));

        var result = mapper.toResult(notification);

        assertThat(result.status()).isEqualTo("DELIVERED");
        assertThat(result.deliveredAt()).isEqualTo(NOW.plusSeconds(2));
    }

    @Test
    void mapsAFailedNotificationWithItsFailureReason() {
        Notification notification = newNotification();
        notification.startDelivery(AggregateId.newId(), NOW);
        notification.markFailed("provider-unreachable", AggregateId.newId(), NOW.plusSeconds(1));

        var result = mapper.toResult(notification);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.failureReason()).isEqualTo("provider-unreachable");
        assertThat(result.deliveredAt()).isNull();
    }

    @Test
    void mapsNotificationToSummary() {
        Notification notification = newNotification();

        var summary = mapper.toSummary(notification);

        assertThat(summary.notificationId()).isEqualTo(notification.id().value());
        assertThat(summary.channel()).isEqualTo("EMAIL");
        assertThat(summary.status()).isEqualTo("QUEUED");
    }

    @Test
    void rejectsNullInputs() {
        assertThatThrownBy(() -> mapper.toResult(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toSummary(null)).isInstanceOf(NullPointerException.class);
    }

    private Notification newNotification() {
        return Notification.queue(
                AggregateId.newId(), AggregateId.newId(),
                new NotificationRecipient(AggregateId.newId(), "member@example.com"),
                NotificationChannel.EMAIL, NotificationCategory.VERIFICATION,
                new NotificationContent("Account verification", "Please verify your account."),
                NOW, AggregateId.newId(), NOW);
    }
}
