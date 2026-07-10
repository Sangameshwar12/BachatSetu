package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.storage.StoredFileJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoredFileSpringDataRepository extends BaseJpaRepository<StoredFileJpaEntity> {

    Optional<StoredFileJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

    long countByDeletedFalse();

    @Query("SELECT COALESCE(SUM(f.size), 0) FROM StoredFileJpaEntity f WHERE f.deleted = false")
    long sumSize();

    /** One row per provider: {@code [StorageProvider, count]}, for platform analytics only. */
    @Query("SELECT f.provider, COUNT(f) FROM StoredFileJpaEntity f WHERE f.deleted = false GROUP BY f.provider")
    List<Object[]> findProviderDistribution();

    /**
     * One row per day since {@code since}: {@code [year, month, day, count]}, for platform analytics only.
     */
    @Query("""
            SELECT EXTRACT(YEAR FROM f.uploadedAt), EXTRACT(MONTH FROM f.uploadedAt), EXTRACT(DAY FROM f.uploadedAt),
                   COUNT(f)
              FROM StoredFileJpaEntity f
             WHERE f.deleted = false
               AND f.uploadedAt >= :since
             GROUP BY EXTRACT(YEAR FROM f.uploadedAt), EXTRACT(MONTH FROM f.uploadedAt), EXTRACT(DAY FROM f.uploadedAt)
             ORDER BY EXTRACT(YEAR FROM f.uploadedAt), EXTRACT(MONTH FROM f.uploadedAt), EXTRACT(DAY FROM f.uploadedAt)
            """)
    List<Object[]> findDailyUploadTrend(@Param("since") Instant since);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StoredFileJpaEntity fileEntity
               SET fileEntity.deleted = true,
                   fileEntity.deletedAt = :deletedAt,
                   fileEntity.version = fileEntity.version + 1
             WHERE fileEntity.tenantId = :tenantId
               AND fileEntity.id = :fileId
               AND fileEntity.deleted = false
            """)
    int softDelete(
            @Param("tenantId") UUID tenantId,
            @Param("fileId") UUID fileId,
            @Param("deletedAt") Instant deletedAt);
}
