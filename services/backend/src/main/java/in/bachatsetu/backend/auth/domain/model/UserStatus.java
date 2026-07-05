package in.bachatsetu.backend.auth.domain.model;

/** Authentication lifecycle states for a user. */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    SUSPENDED,
    DISABLED
}
