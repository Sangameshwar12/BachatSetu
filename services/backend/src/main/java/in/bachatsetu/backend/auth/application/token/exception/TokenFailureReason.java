package in.bachatsetu.backend.auth.application.token.exception;

public enum TokenFailureReason {
    USER_NOT_FOUND,
    USER_NOT_ACTIVE,
    ROLE_NOT_FOUND,
    PERMISSION_NOT_FOUND,
    ACTIVE_REFRESH_TOKEN_EXISTS,
    REFRESH_TOKEN_NOT_FOUND,
    INVALID_REFRESH_TOKEN,
    REFRESH_TOKEN_EXPIRED,
    REFRESH_TOKEN_REUSED,
    REFRESH_TOKEN_REVOKED
}
