package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.port.RefreshTokenRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RefreshTokenJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.RefreshTokenJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.RefreshTokenSpringDataRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenSpringDataRepository repository;
    private final RefreshTokenJpaMapper mapper;
    private final JpaReferenceProvider references;

    public RefreshTokenRepositoryAdapter(
            RefreshTokenSpringDataRepository repository,
            RefreshTokenJpaMapper mapper,
            JpaReferenceProvider references) {
        this.repository = repository;
        this.mapper = mapper;
        this.references = references;
    }

    @Override
    public Optional<RefreshToken> findById(RefreshTokenId refreshTokenId) {
        return repository.findByIdAndDeletedFalse(refreshTokenId.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(RefreshToken refreshToken) {
        RepositoryOperations.execute(() -> {
            Optional<RefreshTokenJpaEntity> existing = repository.findById(refreshToken.refreshTokenId().value());
            RefreshTokenJpaEntity candidate = mapper.toEntity(refreshToken, references);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }
}
