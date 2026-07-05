package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.auth.domain.model.PermissionId;
import in.bachatsetu.backend.auth.domain.model.Role;
import in.bachatsetu.backend.auth.domain.model.RoleId;
import in.bachatsetu.backend.infrastructure.persistence.entity.PersistenceRecordStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.PermissionJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RoleJpaEntity;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface RoleJpaMapper {

    default Role toDomain(RoleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Set<PermissionId> permissionIds = entity.getPermissions().stream()
                .map(PermissionJpaEntity::getId)
                .map(PermissionId::new)
                .collect(Collectors.toUnmodifiableSet());
        return Role.rehydrate(
                new RoleId(entity.getId()),
                entity.getCode(),
                permissionIds,
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default RoleJpaEntity toEntity(Role domain, Set<PermissionJpaEntity> permissions) {
        RoleJpaEntity entity = new RoleJpaEntity(
                domain.roleId().value(),
                null,
                domain.name(),
                domain.name(),
                "PLATFORM",
                null,
                PersistenceRecordStatus.ACTIVE);
        entity.replacePermissions(permissions);
        return entity;
    }

    default void updateEntity(RoleJpaEntity entity, Set<PermissionJpaEntity> permissions) {
        entity.replacePermissions(permissions);
    }
}
