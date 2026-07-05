package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.token.port.IssuedRefreshToken;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.application.token.port.TokenHasherPort;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenHash;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Generates 256-bit opaque secrets and persists only their BCrypt representation. */
public final class BCryptTokenHasherAdapter implements TokenHasherPort {

    private static final int SECRET_BYTES = 32;

    private final SecureRandom secureRandom;
    private final BCryptPasswordEncoder encoder;

    public BCryptTokenHasherAdapter(
            SecureRandom secureRandom,
            BCryptPasswordEncoder encoder) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secure random must not be null");
        this.encoder = Objects.requireNonNull(encoder, "BCrypt encoder must not be null");
    }

    @Override
    public IssuedRefreshToken issue(RefreshTokenId tokenId) {
        Objects.requireNonNull(tokenId, "refresh token id must not be null");
        byte[] randomBytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(randomBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        RefreshTokenCredential credential = RefreshTokenCredential.create(tokenId, secret);
        return new IssuedRefreshToken(
                credential,
                RefreshTokenHash.encoded(encoder.encode(secret)));
    }

    @Override
    public boolean matches(
            RefreshTokenCredential candidate,
            RefreshTokenHash persistedHash) {
        Objects.requireNonNull(candidate, "refresh token candidate must not be null");
        Objects.requireNonNull(persistedHash, "refresh token hash must not be null");
        return encoder.matches(candidate.secret(), persistedHash.value());
    }
}
