package in.bachatsetu.backend.infrastructure.persistence.exception;

public final class PersistenceResourceNotFoundException extends PersistenceException {

    public PersistenceResourceNotFoundException(String message) {
        super(message);
    }

    public PersistenceResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
