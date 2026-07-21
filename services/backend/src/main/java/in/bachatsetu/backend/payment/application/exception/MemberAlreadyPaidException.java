package in.bachatsetu.backend.payment.application.exception;

/** Raised when a manual payment is recorded for a member who has already paid for the current cycle. */
public final class MemberAlreadyPaidException extends PaymentApplicationException {

    public MemberAlreadyPaidException(String message) {
        super(message);
    }
}
