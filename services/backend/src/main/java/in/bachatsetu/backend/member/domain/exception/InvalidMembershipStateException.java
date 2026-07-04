package in.bachatsetu.backend.member.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidMembershipStateException extends DomainException {

    public InvalidMembershipStateException(String message) {
        super(message);
    }
}
