package in.bachatsetu.backend.auth.application.token.usecase;

import in.bachatsetu.backend.auth.application.token.command.GenerateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;

@FunctionalInterface
public interface GenerateAccessTokenUseCase {

    IssuedAccessToken generate(GenerateAccessTokenCommand command);
}
