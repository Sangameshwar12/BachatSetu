package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.MemberJpaEntity;
import in.bachatsetu.backend.member.domain.model.GroupParticipation;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.model.MemberStatus;
import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface MemberJpaMapper {

    default MemberProfile toDomain(MemberJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        GroupParticipation participation = new GroupParticipation(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getGroup().getId()),
                entity.getRole(),
                entity.getJoinedAt(),
                entity.getStatus(),
                entity.getExitedAt());
        return new MemberProfile(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                JpaMappingSupport.id(entity.getUser().getId()),
                new MemberNumber(entity.getMemberNumber()),
                MemberStatus.valueOf(entity.getStatus().name()),
                List.of(participation),
                List.of(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default MemberJpaEntity toEntity(
            MemberProfile domain,
            GroupParticipation participation,
            @Context JpaReferenceProvider references) {
        if (domain == null || participation == null) {
            return null;
        }
        return new MemberJpaEntity(
                participation.id().value(),
                domain.tenantId().value(),
                references.group(participation.groupId()),
                references.user(domain.userId()),
                domain.memberNumber().value(),
                participation.role(),
                participation.status(),
                participation.joinedAt(),
                participation.exitedAt());
    }
}
