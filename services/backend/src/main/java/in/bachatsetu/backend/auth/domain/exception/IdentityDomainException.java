package in.bachatsetu.backend.auth.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

/**
 * Signals a violation of an authentication or identity aggregate invariant.
 */
public final class IdentityDomainException extends DomainException {

    /**
     * Creates an identity-domain exception.
     *
     * @param message safe invariant violation description
     */
    public IdentityDomainException(String message) {
        super(message);
    }
}
