package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.platform.AnnouncementJpaEntity;
import in.bachatsetu.backend.platformoperations.domain.model.Announcement;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface AnnouncementJpaMapper {

    default Announcement toDomain(AnnouncementJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Announcement.rehydrate(
                JpaMappingSupport.id(entity.getId()), entity.getTitle(), entity.getMessage(), entity.getStartAt(),
                entity.getEndAt(), entity.getSeverity(), JpaMappingSupport.auditInfo(entity), entity.getVersion());
    }

    default AnnouncementJpaEntity toEntity(Announcement domain) {
        if (domain == null) {
            return null;
        }
        return new AnnouncementJpaEntity(
                domain.id().value(), domain.title(), domain.message(), domain.startAt(), domain.endAt(),
                domain.severity());
    }
}
