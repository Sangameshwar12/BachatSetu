package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import in.bachatsetu.backend.notification.domain.model.DeliveryAttempt;
import in.bachatsetu.backend.notification.domain.model.DeliveryAttemptStatus;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.notification.domain.model.NotificationStatus;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface NotificationJpaMapper {

    default Notification toDomain(NotificationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Notification(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                new NotificationRecipient(
                        JpaMappingSupport.id(entity.getUser().getId()),
                        entity.getRecipientReference()),
                entity.getChannel(),
                entity.getCategory(),
                new NotificationContent(entity.getSubject(), entity.getBody()),
                entity.getScheduledAt(),
                entity.getStatus(),
                reconstructAttempts(entity),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    /**
     * Reconstructs at most one synthetic {@link DeliveryAttempt} from the entity's own status, since
     * {@code notification.notifications} has no child table for delivery attempt history. This mirrors
     * receipt persistence's single-synthetic-line reload behavior: it does not restore the original attempt's
     * identity, provider message id, or failure detail (none of those are persisted columns), but it restores
     * enough attempt state that {@code Notification.markDelivered}/{@code markFailed} — which operate on the
     * most recent attempt — remain callable after a reload, rather than failing with no attempts present.
     */
    private List<DeliveryAttempt> reconstructAttempts(NotificationJpaEntity entity) {
        DeliveryAttemptStatus attemptStatus = switch (entity.getStatus()) {
            case QUEUED, CANCELLED -> null;
            case SENDING -> DeliveryAttemptStatus.STARTED;
            case SENT, DELIVERED -> DeliveryAttemptStatus.ACCEPTED;
            case FAILED -> DeliveryAttemptStatus.FAILED;
        };
        if (attemptStatus == null) {
            return List.of();
        }
        Instant startedAt = entity.getSentAt() != null ? entity.getSentAt() : entity.getScheduledAt();
        return List.of(new DeliveryAttempt(AggregateId.newId(), 1, startedAt, attemptStatus, null, null));
    }

    default NotificationJpaEntity toEntity(
            Notification domain,
            int priority,
            @Context JpaReferenceProvider references) {
        if (domain == null) {
            return null;
        }
        boolean sent = domain.status() == NotificationStatus.SENT
                || domain.status() == NotificationStatus.DELIVERED;
        return new NotificationJpaEntity(
                domain.id().value(),
                domain.tenantId().value(),
                references.user(domain.recipient().userId()),
                domain.category(),
                domain.channel(),
                domain.recipient().destination(),
                domain.content().subject(),
                domain.content().body(),
                domain.status(),
                domain.scheduledAt(),
                sent ? domain.auditInfo().updatedAt() : null,
                priority);
    }
}
