package in.bachatsetu.backend.auth.domain.model;

/** Supported business purposes for OTP verification. */
public enum OtpPurpose {
    REGISTRATION,
    SIGN_IN,
    PASSWORD_RESET,
    MOBILE_CHANGE
}
