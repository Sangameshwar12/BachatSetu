package in.bachatsetu.backend.support.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import in.bachatsetu.backend.support.domain.event.SupportTicketAssigned;
import in.bachatsetu.backend.support.domain.event.SupportTicketClosed;
import in.bachatsetu.backend.support.domain.event.SupportTicketCreated;
import in.bachatsetu.backend.support.domain.event.SupportTicketResolved;
import in.bachatsetu.backend.support.domain.exception.SupportTicketDomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A customer-support ticket raised by any authenticated user and worked by the platform team. */
public final class SupportTicket extends BaseAggregateRoot {

    private final AggregateId tenantId;
    private final AggregateId raisedBy;
    private final TicketCategory category;
    private final TicketPriority priority;
    private final String subject;
    private final String description;
    private TicketStatus status;
    private AggregateId assignedTo;
    private Instant resolvedAt;
    private String resolution;

    private SupportTicket(
            AggregateId id,
            AggregateId tenantId,
            AggregateId raisedBy,
            TicketCategory category,
            TicketPriority priority,
            String subject,
            String description,
            TicketStatus status,
            AggregateId assignedTo,
            Instant resolvedAt,
            String resolution,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.raisedBy = Objects.requireNonNull(raisedBy, "raisedBy must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.subject = requireNonBlank(subject, "subject");
        this.description = requireNonBlank(description, "description");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.assignedTo = assignedTo;
        this.resolvedAt = resolvedAt;
        this.resolution = resolution;
    }

    public static SupportTicket create(
            AggregateId id,
            AggregateId tenantId,
            AggregateId raisedBy,
            TicketCategory category,
            TicketPriority priority,
            String subject,
            String description,
            AggregateId actorId,
            Instant createdAt) {
        SupportTicket ticket = new SupportTicket(
                id, tenantId, raisedBy, category, priority, subject, description, TicketStatus.OPEN, null, null,
                null, AuditInfo.createdBy(actorId, createdAt), 0);
        ticket.registerEvent(new SupportTicketCreated(UUID.randomUUID(), id, tenantId, category, priority, createdAt));
        return ticket;
    }

    public static SupportTicket rehydrate(
            AggregateId id,
            AggregateId tenantId,
            AggregateId raisedBy,
            TicketCategory category,
            TicketPriority priority,
            String subject,
            String description,
            TicketStatus status,
            AggregateId assignedTo,
            Instant resolvedAt,
            String resolution,
            AuditInfo auditInfo,
            long version) {
        return new SupportTicket(
                id, tenantId, raisedBy, category, priority, subject, description, status, assignedTo, resolvedAt,
                resolution, auditInfo, version);
    }

    public void assign(AggregateId assigneeId, AggregateId actorId, Instant assignedAt) {
        Objects.requireNonNull(assigneeId, "assigneeId must not be null");
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            throw new SupportTicketDomainException("a resolved or closed ticket cannot be assigned");
        }
        this.assignedTo = assigneeId;
        this.status = TicketStatus.ASSIGNED;
        markChanged(actorId, assignedAt);
        registerEvent(new SupportTicketAssigned(UUID.randomUUID(), id(), assigneeId, assignedAt));
    }

    public void resolve(String resolutionText, AggregateId actorId, Instant resolvedAt) {
        String requiredResolution = requireNonBlank(resolutionText, "resolution");
        if (status == TicketStatus.CLOSED) {
            throw new SupportTicketDomainException("a closed ticket cannot be resolved");
        }
        if (status == TicketStatus.RESOLVED) {
            throw new SupportTicketDomainException("ticket is already resolved");
        }
        this.status = TicketStatus.RESOLVED;
        this.resolution = requiredResolution;
        this.resolvedAt = resolvedAt;
        markChanged(actorId, resolvedAt);
        registerEvent(new SupportTicketResolved(UUID.randomUUID(), id(), resolvedAt));
    }

    public void close(AggregateId actorId, Instant closedAt) {
        if (status != TicketStatus.RESOLVED) {
            throw new SupportTicketDomainException("only a resolved ticket can be closed");
        }
        this.status = TicketStatus.CLOSED;
        markChanged(actorId, closedAt);
        registerEvent(new SupportTicketClosed(UUID.randomUUID(), id(), closedAt));
    }

    public AggregateId tenantId() {
        return tenantId;
    }

    public AggregateId raisedBy() {
        return raisedBy;
    }

    public TicketCategory category() {
        return category;
    }

    public TicketPriority priority() {
        return priority;
    }

    public String subject() {
        return subject;
    }

    public String description() {
        return description;
    }

    public TicketStatus status() {
        return status;
    }

    public AggregateId assignedTo() {
        return assignedTo;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    public String resolution() {
        return resolution;
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
