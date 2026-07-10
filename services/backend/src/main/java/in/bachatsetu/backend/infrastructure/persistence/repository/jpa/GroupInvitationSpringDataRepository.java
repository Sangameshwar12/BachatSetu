package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupInvitationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.invitation.domain.model.InvitationStatus;
import java.util.Optional;
import java.util.UUID;

public interface GroupInvitationSpringDataRepository extends BaseJpaRepository<GroupInvitationJpaEntity> {

    Optional<GroupInvitationJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<GroupInvitationJpaEntity> findByGroupIdAndStatusAndDeletedFalse(UUID groupId, InvitationStatus status);

    Optional<GroupInvitationJpaEntity> findByTenantIdAndInvitationCodeAndDeletedFalse(
            UUID tenantId, String invitationCode);

    Optional<GroupInvitationJpaEntity> findBySecureTokenAndDeletedFalse(String secureToken);
}
