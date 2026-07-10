package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupInvitationJpaEntity;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import in.bachatsetu.backend.shared.domain.AggregateId;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface GroupInvitationJpaMapper {

    default GroupInvitation toDomain(GroupInvitationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return GroupInvitation.rehydrate(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                JpaMappingSupport.id(entity.getGroupId()),
                new InvitationCode(entity.getInvitationCode()),
                new InvitationToken(entity.getSecureToken()),
                entity.getInvitationType(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getAcceptedAt(),
                entity.getAcceptedBy() == null ? null : new AggregateId(entity.getAcceptedBy()),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default GroupInvitationJpaEntity toEntity(GroupInvitation domain) {
        if (domain == null) {
            return null;
        }
        return new GroupInvitationJpaEntity(
                domain.id().value(),
                domain.tenantId().value(),
                domain.groupId().value(),
                domain.code().value(),
                domain.token().value(),
                domain.type(),
                domain.status(),
                domain.expiresAt(),
                domain.acceptedAt(),
                domain.acceptedBy() == null ? null : domain.acceptedBy().value());
    }
}
