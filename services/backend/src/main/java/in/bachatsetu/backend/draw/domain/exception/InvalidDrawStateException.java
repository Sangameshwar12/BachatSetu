package in.bachatsetu.backend.draw.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidDrawStateException extends DomainException {

    public InvalidDrawStateException(String message) {
        super(message);
    }
}
