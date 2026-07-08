package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.storage.StoredFileJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoredFileSpringDataRepository extends BaseJpaRepository<StoredFileJpaEntity> {

    Optional<StoredFileJpaEntity> findByTenantIdAndIdAndDeletedFalse(UUID tenantId, UUID id);

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
