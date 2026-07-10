package in.bachatsetu.backend.user.interfaces.rest.adapter;

import in.bachatsetu.backend.shared.domain.DomainEvent;
import in.bachatsetu.backend.user.application.port.DomainEventPublisherPort;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

public final class ApplicationEventUserEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public ApplicationEventUserEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    @Override
    public void publish(List<DomainEvent> events) {
        Objects.requireNonNull(events, "events must not be null");
        events.forEach(publisher::publishEvent);
    }
}
