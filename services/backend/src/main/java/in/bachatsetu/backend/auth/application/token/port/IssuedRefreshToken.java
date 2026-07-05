package in.bachatsetu.backend.auth.application.token.port;

import in.bachatsetu.backend.auth.domain.model.RefreshTokenHash;
import java.util.Objects;

/** Ephemeral credential paired with the one-way representation to persist. */
public record IssuedRefreshToken(RefreshTokenCredential credential, RefreshTokenHash hash) {

    public IssuedRefreshToken {
        Objects.requireNonNull(credential, "refresh token credential must not be null");
        Objects.requireNonNull(hash, "refresh token hash must not be null");
    }
}
