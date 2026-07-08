package in.bachatsetu.backend.audit.application.exception;

public final class AuditEntryNotFoundException extends AuditApplicationException {

    public AuditEntryNotFoundException(String message) {
        super(message);
    }
}
