package in.bachatsetu.backend.notification.domain.exception;

import in.bachatsetu.backend.shared.domain.DomainException;

public final class InvalidNotificationStateException extends DomainException {

    public InvalidNotificationStateException(String message) {
        super(message);
    }
}
