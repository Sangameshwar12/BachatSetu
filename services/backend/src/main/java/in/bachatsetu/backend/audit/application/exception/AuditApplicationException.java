package in.bachatsetu.backend.audit.application.exception;

/** Base type for every Audit application-layer failure. */
public class AuditApplicationException extends RuntimeException {

    public AuditApplicationException(String message) {
        super(message);
    }
}
