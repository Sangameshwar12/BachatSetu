package in.bachatsetu.backend.draw.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class DuplicateBidException extends DomainException {

    public DuplicateBidException(String message) {
        super(message);
    }
}
