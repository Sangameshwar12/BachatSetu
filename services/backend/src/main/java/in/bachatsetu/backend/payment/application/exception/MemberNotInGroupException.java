package in.bachatsetu.backend.payment.application.exception;

/** Raised when a collection operation targets a member who is not an active member of the group. */
public final class MemberNotInGroupException extends PaymentApplicationException {

    public MemberNotInGroupException(String message) {
        super(message);
    }
}
