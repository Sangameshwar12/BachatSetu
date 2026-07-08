package in.bachatsetu.backend.storage.application.exception;

/** Thrown when no adapter is registered for a requested {@code StorageProvider}. */
public final class UnsupportedStorageProviderException extends StorageApplicationException {

    public UnsupportedStorageProviderException(String message) {
        super(message);
    }
}
