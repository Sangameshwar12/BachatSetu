package in.bachatsetu.backend.storage.application.exception;

/** Base type for every Storage application-layer failure. */
public class StorageApplicationException extends RuntimeException {

    public StorageApplicationException(String message) {
        super(message);
    }
}
