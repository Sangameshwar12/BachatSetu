package in.bachatsetu.backend.group.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

/** Raised when an operation is incompatible with the current group lifecycle state. */
public final class InvalidGroupStateException extends DomainException {

    public InvalidGroupStateException(String message) {
        super(message);
    }
}
