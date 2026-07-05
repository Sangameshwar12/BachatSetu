package in.bachatsetu.backend.auth.application.token.port;

import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import java.util.Objects;

/** One-time-visible opaque refresh credential in identifier.secret form. */
public final class RefreshTokenCredential {

    private static final String SEPARATOR = ".";

    private final RefreshTokenId tokenId;
    private final String secret;

    private RefreshTokenCredential(RefreshTokenId tokenId, String secret) {
        this.tokenId = Objects.requireNonNull(tokenId, "refresh token id must not be null");
        this.secret = Objects.requireNonNull(secret, "refresh token secret must not be null");
        if (secret.length() < 32 || secret.indexOf('.') >= 0) {
            throw new IllegalArgumentException("refresh token secret format is invalid");
        }
    }

    public static RefreshTokenCredential create(RefreshTokenId tokenId, String secret) {
        return new RefreshTokenCredential(tokenId, secret);
    }

    public static RefreshTokenCredential parse(String value) {
        Objects.requireNonNull(value, "refresh token must not be null");
        int separator = value.indexOf(SEPARATOR);
        if (separator < 1 || separator != value.lastIndexOf(SEPARATOR)) {
            throw new IllegalArgumentException("refresh token format is invalid");
        }
        return create(RefreshTokenId.from(value.substring(0, separator)), value.substring(separator + 1));
    }

    public RefreshTokenId tokenId() {
        return tokenId;
    }

    public String secret() {
        return secret;
    }

    public String value() {
        return tokenId + SEPARATOR + secret;
    }

    @Override
    public String toString() {
        return "[REDACTED]";
    }
}
