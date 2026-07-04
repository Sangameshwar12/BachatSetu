package in.bachatsetu.backend.infrastructure.persistence.exception;

public final class PersistenceMappingException extends PersistenceException {

    public PersistenceMappingException(String message) {
        super(message);
    }

    public PersistenceMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
