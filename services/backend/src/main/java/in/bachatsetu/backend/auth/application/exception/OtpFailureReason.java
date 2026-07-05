package in.bachatsetu.backend.auth.application.exception;

public enum OtpFailureReason {
    USER_NOT_FOUND,
    ACTIVE_OTP_EXISTS,
    OTP_NOT_FOUND,
    RESEND_LIMIT_REACHED
}
