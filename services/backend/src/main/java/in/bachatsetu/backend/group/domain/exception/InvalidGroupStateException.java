package in.bachatsetu.backend.group.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidGroupStateException extends DomainException {

    public InvalidGroupStateException(String message) {
        super(message);
    }
}
