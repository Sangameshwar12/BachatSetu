package in.bachatsetu.backend.support.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SupportTicketCreated(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId tenantId,
        TicketCategory category,
        TicketPriority priority,
        Instant occurredAt) implements DomainEvent {

    public SupportTicketCreated {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
