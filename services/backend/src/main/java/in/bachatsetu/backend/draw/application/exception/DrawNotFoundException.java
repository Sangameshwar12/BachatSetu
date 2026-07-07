package in.bachatsetu.backend.draw.application.exception;

/** Raised when a tenant-scoped draw lookup has no result. */
public final class DrawNotFoundException extends DrawApplicationException {

    public DrawNotFoundException(String message) {
        super(message);
    }
}
