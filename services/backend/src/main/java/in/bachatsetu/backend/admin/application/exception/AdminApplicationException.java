package in.bachatsetu.backend.admin.application.exception;

/** Base type for every Admin application-layer failure. */
public class AdminApplicationException extends RuntimeException {

    public AdminApplicationException(String message) {
        super(message);
    }
}
