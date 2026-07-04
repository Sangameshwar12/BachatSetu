package in.bachatsetu.backend.draw.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidBidStateException extends DomainException {

    public InvalidBidStateException(String message) {
        super(message);
    }
}
