package in.bachatsetu.backend.auth.application.signup.exception;

/** Reasons a signup attempt can be rejected. */
public enum SignupFailureReason {
    TERMS_NOT_ACCEPTED,
    MOBILE_ALREADY_REGISTERED,
    EMAIL_ALREADY_REGISTERED,
    OTP_EXPIRED,
    OTP_INVALID,
    OTP_ATTEMPTS_EXCEEDED
}
