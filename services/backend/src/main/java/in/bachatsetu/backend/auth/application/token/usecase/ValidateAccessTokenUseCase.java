package in.bachatsetu.backend.auth.application.token.usecase;

import in.bachatsetu.backend.auth.application.token.command.ValidateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;

@FunctionalInterface
public interface ValidateAccessTokenUseCase {

    AccessTokenClaims validate(ValidateAccessTokenCommand command);
}
