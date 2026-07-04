package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;

public interface UserSpringDataRepository extends BaseJpaRepository<UserJpaEntity> {

    Optional<UserJpaEntity> findByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<UserJpaEntity> findByPhoneNumberAndDeletedFalse(String phoneNumber);
}
