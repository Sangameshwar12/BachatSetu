package in.bachatsetu.backend.group.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

/** Raised when an operation attempts to remove the group owner. */
public final class OwnerRemovalNotAllowedException extends DomainException {

    public OwnerRemovalNotAllowedException(String message) {
        super(message);
    }
}
