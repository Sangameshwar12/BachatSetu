package in.bachatsetu.backend.member.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class DuplicateGroupParticipationException extends DomainException {

    public DuplicateGroupParticipationException(String message) {
        super(message);
    }
}
