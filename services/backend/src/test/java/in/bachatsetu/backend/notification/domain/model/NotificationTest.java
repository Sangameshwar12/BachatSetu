package in.bachatsetu.backend.notification.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.notification.domain.event.NotificationQueued;
import in.bachatsetu.backend.notification.domain.event.NotificationStatusChanged;
import in.bachatsetu.backend.notification.domain.exception.InvalidNotificationStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void queuesANotificationAndRegistersAnEvent() {
        Notification notification = newNotification();

        assertThat(notification.status()).isEqualTo(NotificationStatus.QUEUED);
        assertThat(notification.attempts()).isEmpty();
        assertThat(notification.pullDomainEvents()).singleElement().isInstanceOf(NotificationQueued.class);
    }

    @Test
    void rejectsSchedulingBeforeTheQueuedTimestamp() {
        assertThatThrownBy(() -> Notification.queue(
                        AggregateId.newId(), AggregateId.newId(), recipient(), NotificationChannel.EMAIL,
                        NotificationCategory.VERIFICATION, content(), NOW.minusSeconds(60), AggregateId.newId(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void progressesThroughTheFullSuccessfulLifecycle() {
        Notification notification = newNotification();
        notification.pullDomainEvents();

        notification.startDelivery(AggregateId.newId(), NOW);
        assertThat(notification.status()).isEqualTo(NotificationStatus.SENDING);
        assertThat(notification.attempts()).singleElement().satisfies(attempt ->
                assertThat(attempt.status()).isEqualTo(DeliveryAttemptStatus.STARTED));

        notification.markSent("provider-msg-1", AggregateId.newId(), NOW.plusSeconds(1));
        assertThat(notification.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.attempts()).singleElement().satisfies(attempt -> {
            assertThat(attempt.status()).isEqualTo(DeliveryAttemptStatus.ACCEPTED);
            assertThat(attempt.providerMessageId()).isEqualTo("provider-msg-1");
        });

        notification.markDelivered(AggregateId.newId(), NOW.plusSeconds(2));
        assertThat(notification.status()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(notification.pullDomainEvents()).hasSize(3)
                .anyMatch(NotificationStatusChanged.class::isInstance);
    }

    @Test
    void marksAFailureDuringSending() {
        Notification notification = newNotification();
        notification.startDelivery(AggregateId.newId(), NOW);

        notification.markFailed("provider-unreachable", AggregateId.newId(), NOW.plusSeconds(1));

        assertThat(notification.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.attempts()).singleElement().satisfies(attempt -> {
            assertThat(attempt.status()).isEqualTo(DeliveryAttemptStatus.FAILED);
            assertThat(attempt.failureCode()).isEqualTo("provider-unreachable");
        });
    }

    @Test
    void marksAFailureAfterSendingSucceededButDeliveryWasNotConfirmed() {
        Notification notification = newNotification();
        notification.startDelivery(AggregateId.newId(), NOW);
        notification.markSent("provider-msg-1", AggregateId.newId(), NOW.plusSeconds(1));

        notification.markFailed("bounced", AggregateId.newId(), NOW.plusSeconds(2));

        assertThat(notification.status()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void restartsDeliveryAfterAPriorFailure() {
        Notification notification = newNotification();
        notification.startDelivery(AggregateId.newId(), NOW);
        notification.markFailed("timeout", AggregateId.newId(), NOW.plusSeconds(1));

        notification.startDelivery(AggregateId.newId(), NOW.plusSeconds(2));

        assertThat(notification.status()).isEqualTo(NotificationStatus.SENDING);
        assertThat(notification.attempts()).hasSize(2);
    }

    @Test
    void rejectsStartingDeliveryFromAnInvalidState() {
        Notification notification = newNotification();
        notification.startDelivery(AggregateId.newId(), NOW);
        notification.markSent("provider-msg-1", AggregateId.newId(), NOW.plusSeconds(1));

        assertThatThrownBy(() -> notification.startDelivery(AggregateId.newId(), NOW.plusSeconds(2)))
                .isInstanceOf(InvalidNotificationStateException.class);
    }

    @Test
    void rejectsMarkingDeliveredFromAnInvalidState() {
        Notification notification = newNotification();

        assertThatThrownBy(() -> notification.markDelivered(AggregateId.newId(), NOW.plusSeconds(1)))
                .isInstanceOf(InvalidNotificationStateException.class);
    }

    @Test
    void rejectsMarkingFailedFromAnInvalidState() {
        Notification notification = newNotification();

        assertThatThrownBy(() -> notification.markFailed("code", AggregateId.newId(), NOW.plusSeconds(1)))
                .isInstanceOf(InvalidNotificationStateException.class);
    }

    private Notification newNotification() {
        return Notification.queue(
                AggregateId.newId(), AggregateId.newId(), recipient(), NotificationChannel.EMAIL,
                NotificationCategory.VERIFICATION, content(), NOW, AggregateId.newId(), NOW);
    }

    private NotificationRecipient recipient() {
        return new NotificationRecipient(AggregateId.newId(), "member@example.com");
    }

    private NotificationContent content() {
        return new NotificationContent("Account verification", "Please verify your account.");
    }
}
