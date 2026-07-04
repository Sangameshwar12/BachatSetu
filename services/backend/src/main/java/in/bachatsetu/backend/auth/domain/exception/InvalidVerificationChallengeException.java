package in.bachatsetu.backend.auth.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidVerificationChallengeException extends DomainException {

    public InvalidVerificationChallengeException(String message) {
        super(message);
    }
}
