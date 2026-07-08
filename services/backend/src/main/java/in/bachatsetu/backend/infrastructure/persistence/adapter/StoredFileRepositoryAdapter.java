package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.storage.StoredFileJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.StoredFileJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class StoredFileRepositoryAdapter implements StorageRepository {

    private final StoredFileSpringDataRepository repository;
    private final StoredFileJpaMapper mapper;

    public StoredFileRepositoryAdapter(StoredFileSpringDataRepository repository, StoredFileJpaMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<StoredFile> findById(AggregateId tenantId, AggregateId fileId) {
        return repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), fileId.value())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(StoredFile file) {
        RepositoryOperations.execute(() -> {
            Optional<StoredFileJpaEntity> existing = repository.findById(file.id().value());
            StoredFileJpaEntity candidate = mapper.toEntity(file);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }

    @Override
    @Transactional
    public void delete(AggregateId tenantId, AggregateId fileId) {
        RepositoryOperations.execute(() -> {
            repository.softDelete(tenantId.value(), fileId.value(), Instant.now());
            return null;
        });
    }
}
