package in.bachatsetu.backend.receipt.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidReceiptStateException extends DomainException {

    public InvalidReceiptStateException(String message) {
        super(message);
    }
}
