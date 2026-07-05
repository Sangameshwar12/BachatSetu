package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RefreshTokenJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.auth.domain.model.TokenStatus;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenSpringDataRepository extends BaseJpaRepository<RefreshTokenJpaEntity> {

    Optional<RefreshTokenJpaEntity> findByIdAndDeletedFalse(UUID id);

    Optional<RefreshTokenJpaEntity> findByUser_IdAndSessionIdAndStatusAndDeletedFalse(
            UUID userId,
            UUID sessionId,
            TokenStatus status);
}
