package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupInvitationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.GroupInvitationJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.GroupInvitationSpringDataRepository;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import in.bachatsetu.backend.invitation.domain.model.InvitationStatus;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unlike its sibling adapters in this package, this one takes {@code tenantId} as a method
 * parameter (supplied by the caller) rather than injecting {@code TenantScopeProvider} — it was
 * previously gated to the "local" profile only by inherited convention, not an actual dependency.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class GroupInvitationRepositoryAdapter implements GroupInvitationRepository {

    private final GroupInvitationSpringDataRepository repository;
    private final GroupInvitationJpaMapper mapper;

    public GroupInvitationRepositoryAdapter(
            GroupInvitationSpringDataRepository repository, GroupInvitationJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void save(GroupInvitation invitation) {
        RepositoryOperations.execute(() -> {
            Optional<GroupInvitationJpaEntity> existing = repository.findById(invitation.id().value());
            GroupInvitationJpaEntity candidate = mapper.toEntity(invitation);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }

    @Override
    public Optional<GroupInvitation> findById(AggregateId tenantId, AggregateId invitationId) {
        return repository.findByIdAndTenantIdAndDeletedFalse(invitationId.value(), tenantId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<GroupInvitation> findById(AggregateId invitationId) {
        return repository.findById(invitationId.value())
                .filter(entity -> !entity.isDeleted())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<GroupInvitation> findActiveByGroup(AggregateId tenantId, AggregateId groupId) {
        return repository.findByGroupIdAndStatusAndDeletedFalse(groupId.value(), InvitationStatus.ACTIVE)
                .filter(entity -> entity.getTenantId().equals(tenantId.value()))
                .map(mapper::toDomain);
    }

    @Override
    public Optional<GroupInvitation> findByCode(AggregateId tenantId, InvitationCode code) {
        return repository.findByTenantIdAndInvitationCodeAndDeletedFalse(tenantId.value(), code.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<GroupInvitation> findByToken(InvitationToken token) {
        return repository.findBySecureTokenAndDeletedFalse(token.value()).map(mapper::toDomain);
    }
}
