package in.bachatsetu.backend.group.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

/** Raised when joining would exceed the configured member capacity. */
public final class GroupCapacityExceededException extends DomainException {

    public GroupCapacityExceededException(String message) {
        super(message);
    }
}
