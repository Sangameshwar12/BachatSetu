package in.bachatsetu.backend.auth.application.token.port;

import in.bachatsetu.backend.auth.domain.model.RefreshTokenHash;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;

/** Secure opaque refresh-token generation, hashing, and matching boundary. */
public interface TokenHasherPort {

    IssuedRefreshToken issue(RefreshTokenId tokenId);

    boolean matches(RefreshTokenCredential candidate, RefreshTokenHash persistedHash);
}
