package in.bachatsetu.backend.receipt.application.exception;

/** Base exception for application-level Receipt failures. */
public class ReceiptApplicationException extends RuntimeException {

    public ReceiptApplicationException(String message) {
        super(message);
    }
}
