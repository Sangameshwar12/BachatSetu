package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberSpringDataRepository extends BaseJpaRepository<GroupMemberJpaEntity> {

    Optional<GroupMemberJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    List<GroupMemberJpaEntity> findAllByTenantIdAndUser_IdAndDeletedFalseOrderByJoinedAtAsc(
            UUID tenantId, UUID userId);

    List<GroupMemberJpaEntity> findAllByUser_IdAndDeletedFalseOrderByJoinedAtAsc(UUID userId);

    Optional<GroupMemberJpaEntity> findFirstByTenantIdAndMemberNumberAndDeletedFalseOrderByJoinedAtAsc(
            UUID tenantId, String memberNumber);

    @Query("""
            SELECT groupMember FROM GroupMemberJpaEntity groupMember
             WHERE groupMember.tenantId = :tenantId
               AND groupMember.deleted = false
               AND groupMember.joinedAt = (
                   SELECT MIN(earliest.joinedAt) FROM GroupMemberJpaEntity earliest
                    WHERE earliest.tenantId = groupMember.tenantId
                      AND earliest.user = groupMember.user
                      AND earliest.deleted = false
               )
            """)
    Page<GroupMemberJpaEntity> findRepresentativeRowsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);
}
