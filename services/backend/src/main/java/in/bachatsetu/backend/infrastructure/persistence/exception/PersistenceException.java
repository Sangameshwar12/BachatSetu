package in.bachatsetu.backend.infrastructure.persistence.exception;

public abstract class PersistenceException extends RuntimeException {

    protected PersistenceException(String message) {
        super(message);
    }

    protected PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
