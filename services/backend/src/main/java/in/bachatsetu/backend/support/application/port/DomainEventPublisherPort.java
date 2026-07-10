package in.bachatsetu.backend.support.application.port;

import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.util.List;

@FunctionalInterface
public interface DomainEventPublisherPort {

    void publish(List<DomainEvent> events);
}
