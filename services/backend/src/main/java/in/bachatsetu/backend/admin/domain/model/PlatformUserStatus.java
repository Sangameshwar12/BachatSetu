package in.bachatsetu.backend.admin.domain.model;

/**
 * Platform-wide view of an authentication user's lifecycle state. Deliberately independent of {@code
 * auth.domain.model.UserStatus} — the Admin module never depends on the Auth module's domain types — but the
 * values mirror it exactly, since it is what the underlying status column stores.
 */
public enum PlatformUserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    SUSPENDED,
    DISABLED
}
