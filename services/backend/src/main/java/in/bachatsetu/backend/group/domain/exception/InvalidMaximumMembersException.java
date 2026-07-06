package in.bachatsetu.backend.group.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

/** Raised when configured member capacity falls outside the supported range. */
public final class InvalidMaximumMembersException extends DomainException {

    public InvalidMaximumMembersException(String message) {
        super(message);
    }
}
