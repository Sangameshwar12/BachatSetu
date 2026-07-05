package in.bachatsetu.backend.auth.application.token.service;

import in.bachatsetu.backend.auth.application.token.exception.TokenApplicationException;
import in.bachatsetu.backend.auth.application.token.exception.TokenFailureReason;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.application.token.port.TokenHasherPort;
import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.port.RefreshTokenRepository;
import java.util.Objects;

/** Resolves and verifies opaque credentials without exposing lookup distinctions. */
public final class RefreshTokenCredentialVerifier {

    private final RefreshTokenRepository repository;
    private final TokenHasherPort hasher;

    public RefreshTokenCredentialVerifier(
            RefreshTokenRepository repository,
            TokenHasherPort hasher) {
        this.repository = Objects.requireNonNull(repository, "refresh token repository must not be null");
        this.hasher = Objects.requireNonNull(hasher, "token hasher must not be null");
    }

    public RefreshToken verify(String rawToken) {
        RefreshTokenCredential credential;
        try {
            credential = RefreshTokenCredential.parse(rawToken);
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
        RefreshToken token = repository.findById(credential.tokenId()).orElseThrow(this::invalid);
        if (!hasher.matches(credential, token.tokenHash())) {
            throw invalid();
        }
        return token;
    }

    private TokenApplicationException invalid() {
        return new TokenApplicationException(
                TokenFailureReason.INVALID_REFRESH_TOKEN,
                "refresh token is invalid");
    }
}
