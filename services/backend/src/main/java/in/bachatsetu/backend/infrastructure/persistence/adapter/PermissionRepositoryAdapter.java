package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.auth.domain.model.Permission;
import in.bachatsetu.backend.auth.domain.model.PermissionId;
import in.bachatsetu.backend.auth.domain.port.PermissionRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.PermissionJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.PermissionJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PermissionSpringDataRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final PermissionSpringDataRepository repository;
    private final PermissionJpaMapper mapper;

    public PermissionRepositoryAdapter(
            PermissionSpringDataRepository repository,
            PermissionJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Permission> findById(PermissionId permissionId) {
        return repository.findByIdAndDeletedFalse(permissionId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Permission> findByName(String name) {
        return repository.findByCodeAndDeletedFalse(name.strip().toUpperCase(Locale.ROOT))
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(Permission permission) {
        RepositoryOperations.execute(() -> {
            PermissionJpaEntity entity = repository.findById(permission.permissionId().value())
                    .map(existing -> {
                        mapper.updateEntity(permission, existing);
                        return existing;
                    })
                    .orElseGet(() -> mapper.toEntity(permission));
            repository.save(entity);
            return null;
        });
    }
}
