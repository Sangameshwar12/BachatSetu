package in.bachatsetu.backend.auth.application.token.query;

import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.TokenStatus;
import java.util.Objects;

public record RefreshTokenState(RefreshTokenId tokenId, TokenStatus status) {

    public RefreshTokenState {
        Objects.requireNonNull(tokenId, "refresh token id must not be null");
        Objects.requireNonNull(status, "refresh token status must not be null");
    }
}
