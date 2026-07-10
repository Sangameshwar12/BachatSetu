package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.platform.TenantJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.TenantJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.TenantSpringDataRepository;
import in.bachatsetu.backend.platformoperations.domain.model.Tenant;
import in.bachatsetu.backend.platformoperations.domain.port.TenantRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class TenantRepositoryAdapter implements TenantRepository {

    private final TenantSpringDataRepository repository;
    private final TenantJpaMapper mapper;

    public TenantRepositoryAdapter(TenantSpringDataRepository repository, TenantJpaMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public void save(Tenant tenant) {
        RepositoryOperations.execute(() -> {
            Optional<TenantJpaEntity> existing = repository.findById(tenant.id().value());
            TenantJpaEntity candidate = mapper.toEntity(tenant);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }

    @Override
    public Optional<Tenant> findById(AggregateId tenantId) {
        return repository.findByIdAndDeletedFalse(tenantId.value()).map(mapper::toDomain);
    }
}
