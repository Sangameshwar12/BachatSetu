package in.bachatsetu.backend.audit.interfaces.rest.adapter;

import in.bachatsetu.backend.audit.application.port.AuditPublisherPort;
import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

/** Publishes a newly created {@link AuditEntry} through Spring's in-process application event bus. */
public final class ApplicationEventAuditPublisherAdapter implements AuditPublisherPort {

    private final ApplicationEventPublisher publisher;

    public ApplicationEventAuditPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    @Override
    public void publish(AuditEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        publisher.publishEvent(entry);
    }
}
