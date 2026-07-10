package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.support.SupportTicketJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.model.TicketStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportTicketSpringDataRepository extends BaseJpaRepository<SupportTicketJpaEntity> {

    /** Cross-tenant, filterable listing — platform support spans every tenant. */
    @Query("""
            SELECT ticket FROM SupportTicketJpaEntity ticket
             WHERE ticket.deleted = false
               AND (:status IS NULL OR ticket.status = :status)
               AND (:priority IS NULL OR ticket.priority = :priority)
               AND (:category IS NULL OR ticket.category = :category)
               AND (:tenantId IS NULL OR ticket.tenantId = :tenantId)
               AND (:raisedBy IS NULL OR ticket.raisedBy = :raisedBy)
               AND (:createdAfter IS NULL OR ticket.createdAt >= :createdAfter)
               AND (:createdBefore IS NULL OR ticket.createdAt <= :createdBefore)
            """)
    Page<SupportTicketJpaEntity> search(
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("category") TicketCategory category,
            @Param("tenantId") UUID tenantId,
            @Param("raisedBy") UUID raisedBy,
            @Param("createdAfter") Instant createdAfter,
            @Param("createdBefore") Instant createdBefore,
            Pageable pageable);
}
