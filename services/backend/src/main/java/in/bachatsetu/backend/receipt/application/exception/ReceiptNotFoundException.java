package in.bachatsetu.backend.receipt.application.exception;

/** Raised when a tenant-scoped receipt lookup has no result. */
public final class ReceiptNotFoundException extends ReceiptApplicationException {

    public ReceiptNotFoundException(String message) {
        super(message);
    }
}
