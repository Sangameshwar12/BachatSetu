package in.bachatsetu.backend.infrastructure.persistence.exception;

public final class PersistenceConflictException extends PersistenceException {

    public PersistenceConflictException(String message) {
        super(message);
    }

    public PersistenceConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
