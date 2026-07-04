package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.MemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberSpringDataRepository extends BaseJpaRepository<MemberJpaEntity> {

    List<MemberJpaEntity> findAllByTenantIdAndUser_IdAndDeletedFalseOrderByJoinedAtAsc(
            UUID tenantId, UUID userId);

    Optional<MemberJpaEntity> findFirstByTenantIdAndMemberNumberAndDeletedFalseOrderByJoinedAtAsc(
            UUID tenantId, String memberNumber);
}
