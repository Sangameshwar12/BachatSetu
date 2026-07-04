package in.bachatsetu.backend.shared.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BaseAggregateRoot {

    private final AggregateId id;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private AuditInfo auditInfo;
    private long version;

    protected BaseAggregateRoot(AggregateId id, AuditInfo auditInfo, long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.auditInfo = Objects.requireNonNull(auditInfo, "auditInfo must not be null");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        this.version = version;
    }

    public final AggregateId id() {
        return id;
    }

    public final AuditInfo auditInfo() {
        return auditInfo;
    }

    public final long version() {
        return version;
    }

    public final List<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public final List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pendingEvents = List.copyOf(domainEvents);
        domainEvents.clear();
        return pendingEvents;
    }

    protected final void registerEvent(DomainEvent event) {
        DomainEvent requiredEvent = Objects.requireNonNull(event, "event must not be null");
        if (!id.equals(requiredEvent.aggregateId())) {
            throw new IllegalArgumentException("event aggregateId must match aggregate id");
        }
        domainEvents.add(requiredEvent);
    }

    protected final void markChanged(AggregateId actorId, Instant changedAt) {
        auditInfo = auditInfo.updatedBy(actorId, changedAt);
        version++;
    }
}
