package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.support.SupportTicketJpaEntity;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.domain.model.SupportTicket;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface SupportTicketJpaMapper {

    default SupportTicket toDomain(SupportTicketJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return SupportTicket.rehydrate(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                JpaMappingSupport.id(entity.getRaisedBy()),
                entity.getCategory(),
                entity.getPriority(),
                entity.getSubject(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getAssignedTo() == null ? null : new AggregateId(entity.getAssignedTo()),
                entity.getResolvedAt(),
                entity.getResolution(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default SupportTicketJpaEntity toEntity(SupportTicket domain) {
        if (domain == null) {
            return null;
        }
        return new SupportTicketJpaEntity(
                domain.id().value(),
                domain.tenantId().value(),
                domain.raisedBy().value(),
                domain.category(),
                domain.priority(),
                domain.subject(),
                domain.description(),
                domain.status(),
                domain.assignedTo() == null ? null : domain.assignedTo().value(),
                domain.resolvedAt(),
                domain.resolution());
    }
}
