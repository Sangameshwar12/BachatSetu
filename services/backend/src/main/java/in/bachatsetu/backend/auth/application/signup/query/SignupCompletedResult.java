package in.bachatsetu.backend.auth.application.signup.query;

import in.bachatsetu.backend.auth.application.token.port.AccessTokenValue;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.time.Instant;
import java.util.Objects;

/** Account activated and ready to use: carries the caller's first access and refresh tokens. */
public record SignupCompletedResult(
        UserId userId,
        AccessTokenValue accessToken,
        Instant accessTokenExpiresAt,
        RefreshTokenCredential refreshToken,
        Instant refreshTokenExpiresAt) {

    public SignupCompletedResult {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        Objects.requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt must not be null");
        Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        Objects.requireNonNull(refreshTokenExpiresAt, "refreshTokenExpiresAt must not be null");
    }
}
