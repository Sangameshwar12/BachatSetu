package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.event.OtpApplicationEvent;
import in.bachatsetu.backend.auth.application.port.OtpEventPublisherPort;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

/** Publishes an {@link OtpApplicationEvent} through Spring's in-process application event bus. */
public final class ApplicationEventOtpPublisherAdapter implements OtpEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public ApplicationEventOtpPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    @Override
    public void publish(OtpApplicationEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        publisher.publishEvent(event);
    }
}
