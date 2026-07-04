package in.bachatsetu.backend.auth.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidAuthAccountStateException extends DomainException {

    public InvalidAuthAccountStateException(String message) {
        super(message);
    }
}
