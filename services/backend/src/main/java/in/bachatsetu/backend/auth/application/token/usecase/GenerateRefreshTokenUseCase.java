package in.bachatsetu.backend.auth.application.token.usecase;

import in.bachatsetu.backend.auth.application.token.command.GenerateRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenResult;

@FunctionalInterface
public interface GenerateRefreshTokenUseCase {

    RefreshTokenResult generate(GenerateRefreshTokenCommand command);
}
