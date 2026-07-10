package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    List<SavingsGroupJpaEntity> findAllByTenantIdAndOrganizer_IdAndDeletedFalse(UUID tenantId, UUID organizerId);

    @EntityGraph(attributePaths = {"organizer", "members", "members.user"})
    @Query("""
            SELECT groupEntity FROM SavingsGroupJpaEntity groupEntity
             WHERE groupEntity.tenantId = :tenantId
               AND groupEntity.deleted = false
               AND (:status IS NULL OR groupEntity.status = :status)
            """)
    Page<SavingsGroupJpaEntity> findPageByTenantIdAndOptionalStatus(
            @Param("tenantId") UUID tenantId,
            @Param("status") GroupStatus status,
            Pageable pageable);

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

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(GroupStatus status);

    long countByTenantIdAndDeletedFalse(UUID tenantId);

    /** Platform-wide new-group count in a window, for the Platform Operations dashboard only. */
    long countByCreatedAtBetween(Instant start, Instant end);

    /**
     * Cross-tenant, filterable listing for platform administration only — deliberately without the
     * {@code organizer}/{@code members} entity graph the tenant-scoped queries above use, since an
     * administrative listing needs only the group's own summary fields.
     */
    @Query("""
            SELECT groupEntity FROM SavingsGroupJpaEntity groupEntity
             WHERE groupEntity.deleted = false
               AND (:status IS NULL OR groupEntity.status = :status)
               AND (:createdAfter IS NULL OR groupEntity.createdAt >= :createdAfter)
               AND (:createdBefore IS NULL OR groupEntity.createdAt <= :createdBefore)
            """)
    Page<SavingsGroupJpaEntity> searchAcrossTenants(
            @Param("status") GroupStatus status,
            @Param("createdAfter") Instant createdAfter,
            @Param("createdBefore") Instant createdBefore,
            Pageable pageable);

    @Query("SELECT AVG(groupEntity.contributionAmountPaise) FROM SavingsGroupJpaEntity groupEntity "
            + "WHERE groupEntity.deleted = false")
    Double findAverageContributionAmountPaise();

    /** One row per calendar month: {@code [year, month, count]}, for platform analytics only. */
    @Query("""
            SELECT EXTRACT(YEAR FROM g.createdAt), EXTRACT(MONTH FROM g.createdAt), COUNT(g)
              FROM SavingsGroupJpaEntity g
             WHERE g.deleted = false
             GROUP BY EXTRACT(YEAR FROM g.createdAt), EXTRACT(MONTH FROM g.createdAt)
             ORDER BY EXTRACT(YEAR FROM g.createdAt), EXTRACT(MONTH FROM g.createdAt)
            """)
    List<Object[]> findMonthlyNewGroupCounts();
}
