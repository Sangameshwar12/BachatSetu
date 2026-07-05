package in.bachatsetu.backend.auth.domain.port;

import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import java.util.Optional;

/** Persistence port for refresh-token lifecycle records. */
public interface RefreshTokenRepository {

    Optional<RefreshToken> findById(RefreshTokenId refreshTokenId);

    void save(RefreshToken refreshToken);
}
