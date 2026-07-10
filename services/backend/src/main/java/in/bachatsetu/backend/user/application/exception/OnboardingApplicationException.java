package in.bachatsetu.backend.user.application.exception;

import java.util.Objects;

/** Signals a profile onboarding attempt was rejected for a specific, recognized business reason. */
public final class OnboardingApplicationException extends RuntimeException {

    private final OnboardingFailureReason reason;

    public OnboardingApplicationException(OnboardingFailureReason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public OnboardingFailureReason reason() {
        return reason;
    }
}
