package in.bachatsetu.backend.auth.application.token.port;

public enum JwtValidationFailure {
    EXPIRED,
    INVALID_SIGNATURE,
    MALFORMED,
    INVALID_CLAIMS,
    UNSUPPORTED
}
