package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.port.OtpVerificationRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.OtpVerificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.OtpVerificationJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.OtpVerificationSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class OtpVerificationRepositoryAdapter implements OtpVerificationRepository {

    private final OtpVerificationSpringDataRepository repository;
    private final OtpVerificationJpaMapper mapper;
    private final JpaReferenceProvider references;

    public OtpVerificationRepositoryAdapter(
            OtpVerificationSpringDataRepository repository,
            OtpVerificationJpaMapper mapper,
            JpaReferenceProvider references) {
        this.repository = repository;
        this.mapper = mapper;
        this.references = references;
    }

    @Override
    public Optional<OtpVerification> findById(AggregateId verificationId) {
        return repository.findByIdAndDeletedFalse(verificationId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<OtpVerification> findActive(UserId userId, OtpPurpose purpose) {
        return repository.findByUser_IdAndPurposeAndStatusAndDeletedFalse(
                        userId.value(), purpose, OtpStatus.PENDING)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(OtpVerification verification) {
        RepositoryOperations.execute(() -> {
            Optional<OtpVerificationJpaEntity> existing = repository.findById(verification.id().value());
            OtpVerificationJpaEntity candidate = mapper.toEntity(verification, references);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }

    @Override
    @Transactional
    public void replace(OtpVerification current, OtpVerification replacement) {
        RepositoryOperations.execute(() -> {
            OtpVerificationJpaEntity currentEntity = mapper.toEntity(current, references);
            repository.saveAndFlush(RepositoryOperations.preserveState(
                    currentEntity, repository.findById(current.id().value())));
            OtpVerificationJpaEntity replacementEntity = mapper.toEntity(replacement, references);
            repository.save(RepositoryOperations.preserveState(
                    replacementEntity, repository.findById(replacement.id().value())));
            return null;
        });
    }
}
