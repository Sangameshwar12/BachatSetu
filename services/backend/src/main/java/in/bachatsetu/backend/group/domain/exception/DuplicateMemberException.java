package in.bachatsetu.backend.group.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

/** Raised when a member has already joined a savings group. */
public final class DuplicateMemberException extends DomainException {

    public DuplicateMemberException(String message) {
        super(message);
    }
}
