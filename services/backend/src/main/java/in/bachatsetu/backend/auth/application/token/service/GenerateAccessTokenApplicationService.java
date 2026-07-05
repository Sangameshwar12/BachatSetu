package in.bachatsetu.backend.auth.application.token.service;

import in.bachatsetu.backend.auth.application.token.command.GenerateAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;
import in.bachatsetu.backend.auth.application.token.port.JwtProviderPort;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateAccessTokenUseCase;
import java.util.Objects;

public final class GenerateAccessTokenApplicationService implements GenerateAccessTokenUseCase {

    private final TokenPrincipalResolver principals;
    private final JwtProviderPort jwtProvider;

    public GenerateAccessTokenApplicationService(
            TokenPrincipalResolver principals,
            JwtProviderPort jwtProvider) {
        this.principals = Objects.requireNonNull(principals, "token principal resolver must not be null");
        this.jwtProvider = Objects.requireNonNull(jwtProvider, "JWT provider must not be null");
    }

    @Override
    public IssuedAccessToken generate(GenerateAccessTokenCommand command) {
        Objects.requireNonNull(command, "generate access token command must not be null");
        return jwtProvider.issue(principals.resolve(command.userId(), command.tenantId()));
    }
}
