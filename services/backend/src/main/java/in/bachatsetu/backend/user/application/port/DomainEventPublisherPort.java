package in.bachatsetu.backend.user.application.port;

import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.util.List;

@FunctionalInterface
public interface DomainEventPublisherPort {

    void publish(List<DomainEvent> events);
}
