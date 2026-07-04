package in.bachatsetu.backend.shared.domain;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    AggregateId aggregateId();

    Instant occurredAt();

    default String eventType() {
        return getClass().getSimpleName();
    }
}
