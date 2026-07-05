package in.bachatsetu.backend.auth.application.token.service;

import in.bachatsetu.backend.auth.application.token.command.GenerateRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.exception.TokenApplicationException;
import in.bachatsetu.backend.auth.application.token.exception.TokenFailureReason;
import in.bachatsetu.backend.auth.application.token.port.IssuedRefreshToken;
import in.bachatsetu.backend.auth.application.token.port.TokenClockPort;
import in.bachatsetu.backend.auth.application.token.port.TokenHasherPort;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenResult;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateRefreshTokenUseCase;
import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.port.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class GenerateRefreshTokenApplicationService implements GenerateRefreshTokenUseCase {

    private final TokenPrincipalResolver principals;
    private final RefreshTokenRepository repository;
    private final TokenHasherPort hasher;
    private final TokenClockPort clock;
    private final Duration lifetime;

    public GenerateRefreshTokenApplicationService(
            TokenPrincipalResolver principals,
            RefreshTokenRepository repository,
            TokenHasherPort hasher,
            TokenClockPort clock,
            Duration lifetime) {
        this.principals = Objects.requireNonNull(principals, "token principal resolver must not be null");
        this.repository = Objects.requireNonNull(repository, "refresh token repository must not be null");
        this.hasher = Objects.requireNonNull(hasher, "token hasher must not be null");
        this.clock = Objects.requireNonNull(clock, "token clock must not be null");
        this.lifetime = requirePositive(lifetime);
    }

    @Override
    public RefreshTokenResult generate(GenerateRefreshTokenCommand command) {
        Objects.requireNonNull(command, "generate refresh token command must not be null");
        principals.resolve(command.userId(), command.tenantId());
        Instant now = clock.now();
        repository.findActive(command.userId(), command.sessionId()).ifPresent(active -> {
            if (!active.expire(command.actorId(), now)) {
                throw new TokenApplicationException(
                        TokenFailureReason.ACTIVE_REFRESH_TOKEN_EXISTS,
                        "an active refresh token already exists for this session");
            }
            repository.save(active);
        });
        RefreshTokenId tokenId = RefreshTokenId.newId();
        IssuedRefreshToken issued = hasher.issue(tokenId);
        RefreshToken token = RefreshToken.issue(
                tokenId,
                command.userId(),
                command.tenantId(),
                command.sessionId(),
                issued.hash(),
                now,
                now.plus(lifetime),
                command.actorId());
        repository.save(token);
        return new RefreshTokenResult(issued.credential(), token.sessionId(), token.expiresAt());
    }

    private static Duration requirePositive(Duration value) {
        Objects.requireNonNull(value, "refresh token lifetime must not be null");
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException("refresh token lifetime must be positive");
        }
        return value;
    }
}
