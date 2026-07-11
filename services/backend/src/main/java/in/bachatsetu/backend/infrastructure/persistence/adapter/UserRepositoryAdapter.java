package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.UserJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.shared.domain.PhoneNumber;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gated on the "local" profile rather than {@code @ConditionalOnBean(TenantScopeProvider.class)};
 * see {@link AuthUserRepositoryAdapter} for the full rationale.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Profile("local")
@Transactional(readOnly = true)
public class UserRepositoryAdapter implements UserRepository {

    private final UserSpringDataRepository repository;
    private final UserJpaMapper mapper;
    private final TenantScopeProvider tenantScopeProvider;

    public UserRepositoryAdapter(
            UserSpringDataRepository repository,
            UserJpaMapper mapper,
            TenantScopeProvider tenantScopeProvider) {
        this.repository = repository;
        this.mapper = mapper;
        this.tenantScopeProvider = tenantScopeProvider;
    }

    @Override
    public Optional<UserProfile> findById(AggregateId userId) {
        return repository.findByIdAndDeletedFalse(userId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<UserProfile> findByEmail(Email email) {
        return repository.findByTenantIdAndEmailIgnoreCaseAndDeletedFalse(
                        tenantScopeProvider.currentTenantId().value(), email.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserProfile> findByPhoneNumber(PhoneNumber phoneNumber) {
        return repository.findByTenantIdAndPhoneNumberAndDeletedFalse(
                        tenantScopeProvider.currentTenantId().value(), phoneNumber.value())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(UserProfile user) {
        RepositoryOperations.execute(() -> {
            Optional<UserJpaEntity> existing = repository.findById(user.id().value());
            AggregateId tenantId = existing
                    .map(UserJpaEntity::getTenantId)
                    .map(AggregateId::new)
                    .orElseGet(tenantScopeProvider::currentTenantId);
            UserJpaEntity candidate = mapper.toEntity(user, tenantId.value());
            existing.ifPresent(candidate::preserveAuthenticationState);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }
}
