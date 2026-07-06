package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access to the complete Savings Group persistence graph. */
public interface SavingsGroupSpringDataRepository extends BaseJpaRepository<SavingsGroupJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"organizer", "members", "members.user"})
    Optional<SavingsGroupJpaEntity> findByIdAndDeletedFalse(UUID id);

    @EntityGraph(attributePaths = {"organizer", "members", "members.user"})
    Optional<SavingsGroupJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    @EntityGraph(attributePaths = {"organizer", "members", "members.user"})
    Optional<SavingsGroupJpaEntity> findByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);

    boolean existsByTenantIdAndCodeAndDeletedFalse(UUID tenantId, String code);

    @EntityGraph(attributePaths = {"organizer", "members", "members.user"})
    List<SavingsGroupJpaEntity> findDistinctByTenantIdAndDeletedFalseOrderByCreatedAtAsc(UUID tenantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE SavingsGroupJpaEntity groupEntity
               SET groupEntity.deleted = true,
                   groupEntity.deletedAt = :deletedAt,
                   groupEntity.version = groupEntity.version + 1
             WHERE groupEntity.tenantId = :tenantId
               AND groupEntity.id = :groupId
               AND groupEntity.deleted = false
            """)
    int softDelete(
            @Param("tenantId") UUID tenantId,
            @Param("groupId") UUID groupId,
            @Param("deletedAt") Instant deletedAt);
}
