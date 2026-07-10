package in.bachatsetu.backend.platformoperations.interfaces.rest.adapter;

import in.bachatsetu.backend.platformoperations.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

public final class ApplicationEventPlatformOperationsEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public ApplicationEventPlatformOperationsEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    @Override
    public void publish(List<DomainEvent> events) {
        Objects.requireNonNull(events, "events must not be null");
        events.forEach(publisher::publishEvent);
    }
}
