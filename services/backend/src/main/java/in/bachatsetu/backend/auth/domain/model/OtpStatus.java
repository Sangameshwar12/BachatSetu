package in.bachatsetu.backend.auth.domain.model;

/** Lifecycle states for an OTP verification. */
public enum OtpStatus {
    PENDING,
    VERIFIED,
    FAILED,
    EXPIRED,
    INVALIDATED
}
