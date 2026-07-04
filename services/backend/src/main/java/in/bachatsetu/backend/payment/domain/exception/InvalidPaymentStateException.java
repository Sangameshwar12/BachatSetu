package in.bachatsetu.backend.payment.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidPaymentStateException extends DomainException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
