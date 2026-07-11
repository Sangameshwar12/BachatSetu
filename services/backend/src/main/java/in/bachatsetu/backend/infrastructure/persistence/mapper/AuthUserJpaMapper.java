package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.RoleId;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RoleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceMappingException;
import in.bachatsetu.backend.shared.domain.Email;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface AuthUserJpaMapper {

    default User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getEmail() == null || entity.getPhoneNumber() == null
                || entity.getPasswordHash() == null || entity.getAuthenticationStatus() == null) {
            throw new PersistenceMappingException("persisted user is not configured for authentication");
        }
        Set<RoleId> roleIds = entity.getRoles().stream()
                .map(RoleJpaEntity::getId)
                .map(RoleId::new)
                .collect(Collectors.toUnmodifiableSet());
        return User.rehydrate(
                new UserId(entity.getId()),
                new Email(entity.getEmail()),
                MobileNumber.of(entity.getPhoneNumber()),
                PasswordHash.encoded(entity.getPasswordHash()),
                entity.getAuthenticationStatus(),
                roleIds,
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default void updateEntity(User domain, UserJpaEntity entity, Set<RoleJpaEntity> roles) {
        entity.updateAuthentication(
                domain.email().value(), domain.passwordHash().value(), domain.status(), roles);
    }
}
