package in.bachatsetu.backend.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import in.bachatsetu.backend.notification.domain.model.DeliveryAttemptStatus;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.model.NotificationStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class NotificationJpaMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private final NotificationJpaMapper mapper = Mappers.getMapper(NotificationJpaMapper.class);

    @Test
    void mapsAQueuedEntityWithNoDeliveryAttempts() {
        Notification domain = mapper.toDomain(entity(NotificationStatus.QUEUED, null));

        assertThat(domain.status()).isEqualTo(NotificationStatus.QUEUED);
        assertThat(domain.attempts()).isEmpty();
    }

    @Test
    void reconstructsAStartedAttemptForASendingEntity() {
        Notification domain = mapper.toDomain(entity(NotificationStatus.SENDING, null));

        assertThat(domain.attempts()).singleElement().satisfies(attempt ->
                assertThat(attempt.status()).isEqualTo(DeliveryAttemptStatus.STARTED));
    }

    @Test
    void reconstructsAnAcceptedAttemptForASentEntitySoItCanStillBeMarkedDelivered() {
        Notification domain = mapper.toDomain(entity(NotificationStatus.SENT, NOW.plusSeconds(5)));

        assertThat(domain.attempts()).singleElement().satisfies(attempt ->
                assertThat(attempt.status()).isEqualTo(DeliveryAttemptStatus.ACCEPTED));

        domain.markDelivered(in.bachatsetu.backend.shared.domain.AggregateId.newId(), NOW.plusSeconds(10));
        assertThat(domain.status()).isEqualTo(NotificationStatus.DELIVERED);
    }

    @Test
    void reconstructsAnAcceptedAttemptForADeliveredEntity() {
        Notification domain = mapper.toDomain(entity(NotificationStatus.DELIVERED, NOW.plusSeconds(5)));

        assertThat(domain.attempts()).singleElement().satisfies(attempt ->
                assertThat(attempt.status()).isEqualTo(DeliveryAttemptStatus.ACCEPTED));
    }

    @Test
    void reconstructsAFailedAttemptForAFailedEntity() {
        Notification domain = mapper.toDomain(entity(NotificationStatus.FAILED, null));

        assertThat(domain.attempts()).singleElement().satisfies(attempt ->
                assertThat(attempt.status()).isEqualTo(DeliveryAttemptStatus.FAILED));
    }

    @Test
    void mapsACancelledEntityWithNoDeliveryAttempts() {
        Notification domain = mapper.toDomain(entity(NotificationStatus.CANCELLED, null));

        assertThat(domain.attempts()).isEmpty();
    }

    @Test
    void returnsNullForANullEntity() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    private NotificationJpaEntity entity(NotificationStatus status, Instant sentAt) {
        NotificationJpaEntity entity = mock(NotificationJpaEntity.class);
        UserJpaEntity user = mock(UserJpaEntity.class);
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(user.getId()).thenReturn(recipientId);
        when(entity.getId()).thenReturn(UUID.randomUUID());
        when(entity.getTenantId()).thenReturn(UUID.randomUUID());
        when(entity.getUser()).thenReturn(user);
        when(entity.getRecipientReference()).thenReturn("member@example.com");
        when(entity.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(entity.getCategory()).thenReturn(NotificationCategory.VERIFICATION);
        when(entity.getSubject()).thenReturn("Account verification");
        when(entity.getBody()).thenReturn("Please verify your account.");
        when(entity.getStatus()).thenReturn(status);
        when(entity.getScheduledAt()).thenReturn(NOW);
        when(entity.getSentAt()).thenReturn(sentAt);
        when(entity.getCreatedAt()).thenReturn(NOW);
        when(entity.getUpdatedAt()).thenReturn(NOW.plusSeconds(1));
        when(entity.getCreatedBy()).thenReturn(actorId);
        when(entity.getUpdatedBy()).thenReturn(actorId);
        when(entity.getVersion()).thenReturn(0L);
        return entity;
    }
}
