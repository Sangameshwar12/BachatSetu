package in.bachatsetu.backend.audit.application.port;

import in.bachatsetu.backend.audit.domain.model.AuditEntry;

/**
 * Publishes a newly created {@link AuditEntry} for any interested listener (for example a future live audit
 * feed or analytics consumer) — distinct from a domain-event publisher because {@code AuditEntry} itself
 * never registers domain events; the entry being created is, in its entirety, the thing worth publishing.
 */
@FunctionalInterface
public interface AuditPublisherPort {

    void publish(AuditEntry entry);
}
