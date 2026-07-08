package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.infrastructure.persistence.entity.audit.AuditEntryJpaEntity;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface AuditEntryJpaMapper {

    default AuditEntry toDomain(AuditEntryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AuditEntry(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                JpaMappingSupport.id(entity.getActorId()),
                entity.getEventType(),
                entity.getModuleName(),
                entity.getResourceType(),
                JpaMappingSupport.id(entity.getResourceId()),
                entity.getAction(),
                entity.getDescription(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getMetadata(),
                entity.getOccurredAt(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default AuditEntryJpaEntity toEntity(AuditEntry domain) {
        if (domain == null) {
            return null;
        }
        return new AuditEntryJpaEntity(
                domain.id().value(),
                value(domain.tenantId()),
                value(domain.actorId()),
                domain.eventType(),
                domain.moduleName(),
                domain.resourceType(),
                value(domain.resourceId()),
                domain.action(),
                domain.description(),
                domain.ipAddress(),
                domain.userAgent(),
                domain.metadata(),
                domain.createdAt());
    }

    private static UUID value(AggregateId id) {
        return id == null ? null : id.value();
    }
}
