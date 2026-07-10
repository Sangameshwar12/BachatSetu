package in.bachatsetu.backend.support.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

/** Raised when a support ticket operation is incompatible with its current lifecycle state. */
public final class SupportTicketDomainException extends DomainException {

    public SupportTicketDomainException(String message) {
        super(message);
    }
}
