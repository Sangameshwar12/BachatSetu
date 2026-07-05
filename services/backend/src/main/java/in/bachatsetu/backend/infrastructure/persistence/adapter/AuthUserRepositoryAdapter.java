package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.RoleId;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RoleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceResourceNotFoundException;
import in.bachatsetu.backend.infrastructure.persistence.mapper.AuthUserJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.RoleSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.shared.domain.Email;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@ConditionalOnBean(TenantScopeProvider.class)
@Transactional(readOnly = true)
public class AuthUserRepositoryAdapter implements UserRepository {

    private final UserSpringDataRepository repository;
    private final RoleSpringDataRepository roleRepository;
    private final AuthUserJpaMapper mapper;
    private final TenantScopeProvider tenantScopeProvider;

    public AuthUserRepositoryAdapter(
            UserSpringDataRepository repository,
            RoleSpringDataRepository roleRepository,
            AuthUserJpaMapper mapper,
            TenantScopeProvider tenantScopeProvider) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.mapper = mapper;
        this.tenantScopeProvider = tenantScopeProvider;
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return repository.findByIdAndTenantIdAndDeletedFalse(
                        userId.value(), tenantScopeProvider.currentTenantId().value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByTenantIdAndEmailIgnoreCaseAndDeletedFalse(
                        tenantScopeProvider.currentTenantId().value(), email.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByMobileNumber(MobileNumber mobileNumber) {
        return repository.findByTenantIdAndPhoneNumberAndDeletedFalse(
                        tenantScopeProvider.currentTenantId().value(), mobileNumber.value())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(User user) {
        RepositoryOperations.execute(() -> {
            UserJpaEntity entity = repository.findByIdAndTenantIdAndDeletedFalse(
                            user.userId().value(), tenantScopeProvider.currentTenantId().value())
                    .orElseThrow(() -> new PersistenceResourceNotFoundException(
                            "authentication user requires an existing canonical user record"));
            mapper.updateEntity(user, entity, resolveRoles(user.roleIds()));
            repository.save(entity);
            return null;
        });
    }

    private Set<RoleJpaEntity> resolveRoles(Set<RoleId> roleIds) {
        Set<RoleJpaEntity> roles = new LinkedHashSet<>(roleRepository.findAllById(
                roleIds.stream().map(RoleId::value).toList()));
        java.util.UUID tenantId = tenantScopeProvider.currentTenantId().value();
        boolean inaccessibleRole = roles.stream().anyMatch(role -> role.isDeleted()
                || role.getTenantId() != null && !role.getTenantId().equals(tenantId));
        if (roles.size() != roleIds.size() || inaccessibleRole) {
            throw new PersistenceResourceNotFoundException("one or more authentication roles do not exist");
        }
        return roles;
    }
}
