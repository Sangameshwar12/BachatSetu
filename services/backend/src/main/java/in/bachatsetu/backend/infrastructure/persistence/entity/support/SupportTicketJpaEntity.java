package in.bachatsetu.backend.infrastructure.persistence.entity.support;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.model.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Canonical persistence representation of the SupportTicket aggregate root. */
@Entity
@Table(name = "support_tickets", schema = "support")
public class SupportTicketJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @Column(name = "raised_by", nullable = false, updatable = false)
    private UUID raisedBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20, updatable = false)
    private TicketCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10, updatable = false)
    private TicketPriority priority;

    @NotBlank
    @Column(name = "subject", nullable = false, length = 200, updatable = false)
    private String subject;

    @NotBlank
    @Column(name = "description", nullable = false, updatable = false)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution")
    private String resolution;

    protected SupportTicketJpaEntity() {
    }

    public SupportTicketJpaEntity(
            UUID id,
            UUID tenantId,
            UUID raisedBy,
            TicketCategory category,
            TicketPriority priority,
            String subject,
            String description,
            TicketStatus status,
            UUID assignedTo,
            Instant resolvedAt,
            String resolution) {
        super(id);
        this.tenantId = tenantId;
        this.raisedBy = raisedBy;
        this.category = category;
        this.priority = priority;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.assignedTo = assignedTo;
        this.resolvedAt = resolvedAt;
        this.resolution = resolution;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getRaisedBy() {
        return raisedBy;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public void update(TicketStatus newStatus, UUID newAssignedTo, Instant newResolvedAt, String newResolution) {
        this.status = newStatus;
        this.assignedTo = newAssignedTo;
        this.resolvedAt = newResolvedAt;
        this.resolution = newResolution;
    }
}
