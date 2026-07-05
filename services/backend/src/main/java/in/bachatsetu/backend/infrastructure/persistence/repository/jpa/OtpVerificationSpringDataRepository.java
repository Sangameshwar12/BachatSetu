package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.OtpVerificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationSpringDataRepository extends BaseJpaRepository<OtpVerificationJpaEntity> {

    Optional<OtpVerificationJpaEntity> findByUser_IdAndPurposeAndStatusAndDeletedFalse(
            UUID userId, OtpPurpose purpose, OtpStatus status);
}
