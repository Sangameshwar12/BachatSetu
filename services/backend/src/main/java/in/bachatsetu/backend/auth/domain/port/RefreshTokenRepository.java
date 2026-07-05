package in.bachatsetu.backend.auth.domain.port;

import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.util.Optional;

/** Persistence port for refresh-token lifecycle records. */
public interface RefreshTokenRepository {

    Optional<RefreshToken> findById(RefreshTokenId refreshTokenId);

    Optional<RefreshToken> findActive(UserId userId, TokenSessionId sessionId);

    void save(RefreshToken refreshToken);

    void replace(RefreshToken current, RefreshToken replacement);

    void recordReuse(RefreshToken reused, Optional<RefreshToken> activeReplacement);
}
