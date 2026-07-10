package in.bachatsetu.backend.invitation.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

/** Raised when an invitation operation is incompatible with its current lifecycle state. */
public final class InvitationDomainException extends DomainException {

    public InvitationDomainException(String message) {
        super(message);
    }
}
