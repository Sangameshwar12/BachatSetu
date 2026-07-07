package in.bachatsetu.backend.draw.application.exception;

/** Base exception for application-level Draw failures. */
public class DrawApplicationException extends RuntimeException {

    public DrawApplicationException(String message) {
        super(message);
    }
}
