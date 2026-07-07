package in.bachatsetu.backend.payment.interfaces.rest.adapter;

import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

/** Republishes committed Payment domain events through the Spring event bus. */
public final class ApplicationEventPaymentEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public ApplicationEventPaymentEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    @Override
    public void publish(List<DomainEvent> events) {
        Objects.requireNonNull(events, "events must not be null");
        events.forEach(publisher::publishEvent);
    }
}
