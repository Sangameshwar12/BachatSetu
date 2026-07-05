package in.bachatsetu.backend.auth.application.token.port;

import java.time.Instant;
import java.util.Objects;

/** Newly issued access token and its validated claim projection. */
public record IssuedAccessToken(AccessTokenValue token, AccessTokenClaims claims) {

    public IssuedAccessToken {
        Objects.requireNonNull(token, "access token must not be null");
        Objects.requireNonNull(claims, "access token claims must not be null");
    }

    public Instant expiresAt() {
        return claims.expiresAt();
    }
}
