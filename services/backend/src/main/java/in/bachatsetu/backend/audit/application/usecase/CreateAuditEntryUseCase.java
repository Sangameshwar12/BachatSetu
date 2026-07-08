package in.bachatsetu.backend.audit.application.usecase;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.query.AuditEntryResult;

/**
 * Records one business action. Every other module depends on this interface — never on {@code
 * AuditRepository} or any persistence type directly — to publish an audit entry.
 *
 * <p>Callers are responsible for treating a failure here as best-effort: this use case does not swallow its
 * own exceptions, so a caller whose own business operation must never roll back because of an audit failure
 * needs to invoke it outside its own transaction boundary and catch any exception itself.
 */
@FunctionalInterface
public interface CreateAuditEntryUseCase {

    AuditEntryResult execute(CreateAuditEntryCommand command);
}
