package in.bachatsetu.backend.invitation.interfaces.rest.adapter;

import in.bachatsetu.backend.invitation.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

public final class ApplicationEventInvitationEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public ApplicationEventInvitationEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    @Override
    public void publish(List<DomainEvent> events) {
        Objects.requireNonNull(events, "events must not be null");
        events.forEach(publisher::publishEvent);
    }
}
