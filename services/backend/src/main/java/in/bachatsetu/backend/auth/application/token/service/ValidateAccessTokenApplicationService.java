package in.bachatsetu.backend.auth.application.token.service;

import in.bachatsetu.backend.auth.application.token.command.ValidateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.port.AccessTokenClaims;
import in.bachatsetu.backend.auth.application.token.port.JwtProviderPort;
import in.bachatsetu.backend.auth.application.token.usecase.ValidateAccessTokenUseCase;
import java.util.Objects;

public final class ValidateAccessTokenApplicationService implements ValidateAccessTokenUseCase {

    private final JwtProviderPort jwtProvider;

    public ValidateAccessTokenApplicationService(JwtProviderPort jwtProvider) {
        this.jwtProvider = Objects.requireNonNull(jwtProvider, "JWT provider must not be null");
    }

    @Override
    public AccessTokenClaims validate(ValidateAccessTokenCommand command) {
        Objects.requireNonNull(command, "validate access token command must not be null");
        return jwtProvider.validate(command.token());
    }
}
