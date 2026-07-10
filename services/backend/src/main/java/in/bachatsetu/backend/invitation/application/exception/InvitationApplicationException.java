package in.bachatsetu.backend.invitation.application.exception;

import java.util.Objects;

public final class InvitationApplicationException extends RuntimeException {

    private final InvitationFailureReason reason;

    public InvitationApplicationException(InvitationFailureReason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public InvitationFailureReason reason() {
        return reason;
    }
}
