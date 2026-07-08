package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.infrastructure.persistence.entity.audit.AuditEntryJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEntrySpringDataRepository extends BaseJpaRepository<AuditEntryJpaEntity> {

    @Query("""
            SELECT entry FROM AuditEntryJpaEntity entry
             WHERE ((:tenantId IS NULL AND entry.tenantId IS NULL) OR entry.tenantId = :tenantId)
               AND (:actorId IS NULL OR entry.actorId = :actorId)
               AND (:moduleName IS NULL OR entry.moduleName = :moduleName)
               AND (:eventType IS NULL OR entry.eventType = :eventType)
               AND (:dateFrom IS NULL OR entry.occurredAt >= :dateFrom)
               AND (:dateTo IS NULL OR entry.occurredAt <= :dateTo)
               AND entry.deleted = false
            """)
    Page<AuditEntryJpaEntity> search(
            @Param("tenantId") UUID tenantId,
            @Param("actorId") UUID actorId,
            @Param("moduleName") String moduleName,
            @Param("eventType") AuditEventType eventType,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            Pageable pageable);
}
