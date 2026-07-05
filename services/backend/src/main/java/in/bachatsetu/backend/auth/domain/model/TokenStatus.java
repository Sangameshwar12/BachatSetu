package in.bachatsetu.backend.auth.domain.model;

/** Lifecycle states for refresh-token records. */
public enum TokenStatus {
    ACTIVE,
    REVOKED,
    EXPIRED,
    ROTATED,
    REUSED
}
