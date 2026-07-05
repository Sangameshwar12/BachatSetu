package in.bachatsetu.backend.auth.application.token.usecase;

import in.bachatsetu.backend.auth.application.token.command.RevokeRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenState;

@FunctionalInterface
public interface RevokeRefreshTokenUseCase {

    RefreshTokenState revoke(RevokeRefreshTokenCommand command);
}
