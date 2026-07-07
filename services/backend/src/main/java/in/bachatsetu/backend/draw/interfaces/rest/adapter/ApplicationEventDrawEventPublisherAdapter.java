package in.bachatsetu.backend.draw.interfaces.rest.adapter;

import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

/** Republishes committed Draw domain events through the Spring event bus. */
public final class ApplicationEventDrawEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public ApplicationEventDrawEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    @Override
    public void publish(List<DomainEvent> events) {
        Objects.requireNonNull(events, "events must not be null");
        events.forEach(publisher::publishEvent);
    }
}
