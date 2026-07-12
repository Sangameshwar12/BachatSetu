package in.bachatsetu.backend.auth.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.token.command.RefreshAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.command.RevokeRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.exception.TokenApplicationException;
import in.bachatsetu.backend.auth.application.token.exception.TokenFailureReason;
import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.application.token.query.TokenPairResult;
import in.bachatsetu.backend.auth.interfaces.rest.dto.LogoutRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.RefreshTokenRequest;
import in.bachatsetu.backend.auth.interfaces.rest.dto.RefreshTokenResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import org.springframework.stereotype.Component;

/**
 * Maps validated HTTP contracts to token application commands and safe responses. The actor for
 * both refresh and logout is the presented token's own identifier: neither call carries a bearer
 * access token (the caller may not have a live one), so there is no authenticated actor to use —
 * the credential proves its own right to act on its own session, the same way {@link
 * in.bachatsetu.backend.auth.domain.model.RefreshToken#revoke} and {@code markReused} already
 * record {@code RefreshTokenId}-derived actors for self-triggered lifecycle transitions.
 */
@Component
public class TokenApiMapper {

    private static final String BEARER = "Bearer";

    public RefreshAccessTokenCommand toCommand(RefreshTokenRequest request) {
        return new RefreshAccessTokenCommand(request.refreshToken(), actorId(request.refreshToken()));
    }

    public RevokeRefreshTokenCommand toCommand(LogoutRequest request) {
        return new RevokeRefreshTokenCommand(request.refreshToken(), actorId(request.refreshToken()));
    }

    public RefreshTokenResponse toResponse(TokenPairResult result) {
        return new RefreshTokenResponse(
                result.accessToken().claims().userId().toString(),
                result.accessToken().token().value(),
                result.accessToken().expiresAt(),
                result.refreshToken().token().value(),
                result.refreshToken().expiresAt(),
                BEARER);
    }

    private AggregateId actorId(String rawToken) {
        try {
            return RefreshTokenCredential.parse(rawToken).tokenId().toAggregateId();
        } catch (IllegalArgumentException exception) {
            throw new TokenApplicationException(TokenFailureReason.INVALID_REFRESH_TOKEN, "refresh token is invalid");
        }
    }
}
