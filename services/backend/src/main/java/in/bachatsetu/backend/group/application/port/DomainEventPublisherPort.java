package in.bachatsetu.backend.group.application.port;

import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.util.List;

/** Publishes committed aggregate events without prescribing a transport. */
@FunctionalInterface
public interface DomainEventPublisherPort {

    void publish(List<DomainEvent> events);
}
