package in.bachatsetu.backend.auth.application.token.query;

import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import java.time.Instant;
import java.util.Objects;

/** One-time refresh credential response. */
public record RefreshTokenResult(
        RefreshTokenCredential token,
        TokenSessionId sessionId,
        Instant expiresAt) {

    public RefreshTokenResult {
        Objects.requireNonNull(token, "refresh token must not be null");
        Objects.requireNonNull(sessionId, "token session id must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
