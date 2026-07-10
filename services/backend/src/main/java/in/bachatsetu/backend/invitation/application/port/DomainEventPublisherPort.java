package in.bachatsetu.backend.invitation.application.port;

import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.util.List;

@FunctionalInterface
public interface DomainEventPublisherPort {

    void publish(List<DomainEvent> events);
}
