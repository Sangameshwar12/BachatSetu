package in.bachatsetu.backend.admin.domain.model;

/**
 * Platform-wide view of a savings group's lifecycle state. Deliberately independent of {@code
 * group.domain.model.GroupStatus} — the Admin module never depends on the Group module's domain types — but
 * the values mirror it exactly, since it is what the underlying status column stores.
 */
public enum PlatformGroupStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    CLOSED
}
