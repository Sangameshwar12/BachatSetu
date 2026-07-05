package in.bachatsetu.backend.auth.application.token.usecase;

import in.bachatsetu.backend.auth.application.token.command.RefreshAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.query.TokenPairResult;

@FunctionalInterface
public interface RefreshAccessTokenUseCase {

    TokenPairResult refresh(RefreshAccessTokenCommand command);
}
