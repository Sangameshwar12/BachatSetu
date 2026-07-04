package in.bachatsetu.backend.user.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidUserStateException extends DomainException {

    public InvalidUserStateException(String message) {
        super(message);
    }
}
