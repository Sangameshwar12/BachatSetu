package in.bachatsetu.backend.platformoperations.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

/** Raised when a platform operations action is incompatible with its current lifecycle state. */
public final class PlatformOperationsDomainException extends DomainException {

    public PlatformOperationsDomainException(String message) {
        super(message);
    }
}
