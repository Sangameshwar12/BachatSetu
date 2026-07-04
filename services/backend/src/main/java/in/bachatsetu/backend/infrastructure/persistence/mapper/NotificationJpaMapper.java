package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.notification.domain.model.NotificationStatus;
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
                List.of(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
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
