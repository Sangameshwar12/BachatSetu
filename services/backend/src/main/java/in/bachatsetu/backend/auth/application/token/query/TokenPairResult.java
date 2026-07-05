package in.bachatsetu.backend.auth.application.token.query;

import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;
import java.util.Objects;

public record TokenPairResult(IssuedAccessToken accessToken, RefreshTokenResult refreshToken) {

    public TokenPairResult {
        Objects.requireNonNull(accessToken, "access token must not be null");
        Objects.requireNonNull(refreshToken, "refresh token must not be null");
    }
}
