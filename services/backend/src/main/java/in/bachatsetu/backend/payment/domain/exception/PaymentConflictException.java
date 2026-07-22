package in.bachatsetu.backend.payment.domain.exception;

/**
 * Raised when a persistence-layer write for a payment conflicts with another write for the same payment —
 * e.g. two concurrent requests (a double-clicked "Mark paid") both passing the in-application duplicate
 * check before either commits, with the database's idempotency-key constraint then rejecting the second.
 */
public final class PaymentConflictException extends RuntimeException {

    public PaymentConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
