package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.member.domain.model.GroupRole;
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

    /** Cross-tenant member count for platform administration listing only. */
    long countByGroup_IdAndDeletedFalse(UUID groupId);

    /** Cross-tenant total membership row count, for platform analytics only. */
    long countByDeletedFalse();

    /** Distinct-user count for one role, for the Platform Operations dashboard only. */
    @Query("SELECT COUNT(DISTINCT gm.user.id) FROM GroupMemberJpaEntity gm WHERE gm.role = :role AND gm.deleted = false")
    long countDistinctUsersByRoleAndDeletedFalse(@Param("role") GroupRole role);

    /** Every distinct user holding one of the given roles, for Platform Operations broadcast notifications only. */
    @Query("SELECT DISTINCT gm.user FROM GroupMemberJpaEntity gm WHERE gm.role IN :roles AND gm.deleted = false")
    List<UserJpaEntity> findDistinctUsersByRoleInAndDeletedFalse(@Param("roles") List<GroupRole> roles);
}
