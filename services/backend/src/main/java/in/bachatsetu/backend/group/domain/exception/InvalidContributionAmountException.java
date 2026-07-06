package in.bachatsetu.backend.group.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

/** Raised when a monthly contribution falls outside the supported range. */
public final class InvalidContributionAmountException extends DomainException {

    public InvalidContributionAmountException(String message) {
        super(message);
    }
}
