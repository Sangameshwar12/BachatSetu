package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserSpringDataRepository extends BaseJpaRepository<UserJpaEntity> {

    Optional<UserJpaEntity> findByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<UserJpaEntity> findByPhoneNumberAndDeletedFalse(String phoneNumber);

    Optional<UserJpaEntity> findByTenantIdAndEmailIgnoreCaseAndDeletedFalse(UUID tenantId, String email);

    Optional<UserJpaEntity> findByTenantIdAndPhoneNumberAndDeletedFalse(UUID tenantId, String phoneNumber);

    Optional<UserJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
}
