package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.auth.domain.model.PermissionId;
import in.bachatsetu.backend.auth.domain.model.Role;
import in.bachatsetu.backend.auth.domain.model.RoleId;
import in.bachatsetu.backend.auth.domain.port.RoleRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.PermissionJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RoleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceResourceNotFoundException;
import in.bachatsetu.backend.infrastructure.persistence.mapper.RoleJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PermissionSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.RoleSpringDataRepository;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant-scoped via {@link TenantScopeProvider}; see {@link AuthUserRepositoryAdapter} for the
 * full rationale.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleSpringDataRepository repository;
    private final PermissionSpringDataRepository permissionRepository;
    private final RoleJpaMapper mapper;
    private final TenantScopeProvider tenantScopeProvider;

    public RoleRepositoryAdapter(
            RoleSpringDataRepository repository,
            PermissionSpringDataRepository permissionRepository,
            RoleJpaMapper mapper,
            TenantScopeProvider tenantScopeProvider) {
        this.repository = repository;
        this.permissionRepository = permissionRepository;
        this.mapper = mapper;
        this.tenantScopeProvider = tenantScopeProvider;
    }

    @Override
    public Optional<Role> findById(RoleId roleId) {
        return repository.findByIdAndDeletedFalse(roleId.value())
                .filter(this::isVisibleToCurrentTenant)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        String code = name.strip().toUpperCase(Locale.ROOT);
        return repository.findByTenantIdAndCodeAndDeletedFalse(
                        tenantScopeProvider.currentTenantId().value(), code)
                .or(() -> repository.findByTenantIdIsNullAndCodeAndDeletedFalse(code))
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(Role role) {
        RepositoryOperations.execute(() -> {
            Set<PermissionJpaEntity> permissions = resolvePermissions(role.permissionIds());
            RoleJpaEntity entity = repository.findById(role.roleId().value())
                    .map(existing -> {
                        mapper.updateEntity(existing, permissions);
                        return existing;
                    })
                    .orElseGet(() -> mapper.toEntity(role, permissions));
            repository.save(entity);
            return null;
        });
    }

    private Set<PermissionJpaEntity> resolvePermissions(Set<PermissionId> permissionIds) {
        Set<PermissionJpaEntity> permissions = new LinkedHashSet<>(permissionRepository.findAllById(
                permissionIds.stream().map(PermissionId::value).toList()));
        if (permissions.size() != permissionIds.size()
                || permissions.stream().anyMatch(PermissionJpaEntity::isDeleted)) {
            throw new PersistenceResourceNotFoundException("one or more permissions do not exist");
        }
        return permissions;
    }

    private boolean isVisibleToCurrentTenant(RoleJpaEntity role) {
        return role.getTenantId() == null
                || role.getTenantId().equals(tenantScopeProvider.currentTenantId().value());
    }
}
