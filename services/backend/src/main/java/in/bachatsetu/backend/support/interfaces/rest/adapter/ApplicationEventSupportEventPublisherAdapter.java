package in.bachatsetu.backend.support.interfaces.rest.adapter;

import in.bachatsetu.backend.shared.domain.DomainEvent;
import in.bachatsetu.backend.support.application.port.DomainEventPublisherPort;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

public final class ApplicationEventSupportEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public ApplicationEventSupportEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    @Override
    public void publish(List<DomainEvent> events) {
        Objects.requireNonNull(events, "events must not be null");
        events.forEach(publisher::publishEvent);
    }
}
