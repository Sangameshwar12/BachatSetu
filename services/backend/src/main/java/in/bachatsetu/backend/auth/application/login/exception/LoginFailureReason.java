package in.bachatsetu.backend.auth.application.login.exception;

/** Reasons a login attempt can be rejected. */
public enum LoginFailureReason {
    MOBILE_NOT_REGISTERED,
    ACCOUNT_NOT_ACTIVE,
    OTP_EXPIRED,
    OTP_INVALID,
    OTP_ATTEMPTS_EXCEEDED
}
