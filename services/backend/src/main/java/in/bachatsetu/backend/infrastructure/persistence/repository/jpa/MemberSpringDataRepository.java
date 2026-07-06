package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberSpringDataRepository extends BaseJpaRepository<GroupMemberJpaEntity> {

    Optional<GroupMemberJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    List<GroupMemberJpaEntity> findAllByTenantIdAndUser_IdAndDeletedFalseOrderByJoinedAtAsc(
            UUID tenantId, UUID userId);

    Optional<GroupMemberJpaEntity> findFirstByTenantIdAndMemberNumberAndDeletedFalseOrderByJoinedAtAsc(
            UUID tenantId, String memberNumber);
}
