package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.auth.domain.model.Permission;
import in.bachatsetu.backend.auth.domain.model.PermissionId;
import in.bachatsetu.backend.infrastructure.persistence.entity.PersistenceRecordStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.PermissionJpaEntity;
import java.util.Locale;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface PermissionJpaMapper {

    default Permission toDomain(PermissionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Permission.rehydrate(
                new PermissionId(entity.getId()),
                entity.getCode().toLowerCase(Locale.ROOT),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default PermissionJpaEntity toEntity(Permission domain) {
        String code = domain.name().toUpperCase(Locale.ROOT);
        String[] segments = code.split("[.:_-]", 2);
        return new PermissionJpaEntity(
                domain.permissionId().value(),
                code,
                segments[0],
                segments[1],
                null,
                PersistenceRecordStatus.ACTIVE);
    }

    default void updateEntity(Permission domain, PermissionJpaEntity entity) {
        String code = domain.name().toUpperCase(Locale.ROOT);
        String[] segments = code.split("[.:_-]", 2);
        entity.rename(code, segments[0], segments[1]);
    }
}
