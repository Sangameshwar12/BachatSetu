package in.bachatsetu.backend.group.application.exception;

/** Base exception for application-level Savings Group failures. */
public class SavingsGroupApplicationException extends RuntimeException {

    public SavingsGroupApplicationException(String message) {
        super(message);
    }
}
