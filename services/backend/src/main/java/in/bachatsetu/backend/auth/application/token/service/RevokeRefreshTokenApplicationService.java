package in.bachatsetu.backend.auth.application.token.service;

import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auth.application.token.command.RevokeRefreshTokenCommand;
import in.bachatsetu.backend.auth.application.token.exception.TokenApplicationException;
import in.bachatsetu.backend.auth.application.token.exception.TokenFailureReason;
import in.bachatsetu.backend.auth.application.token.port.TokenClockPort;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenState;
import in.bachatsetu.backend.auth.application.token.usecase.RevokeRefreshTokenUseCase;
import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.TokenStatus;
import in.bachatsetu.backend.auth.domain.port.RefreshTokenRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RevokeRefreshTokenApplicationService implements RevokeRefreshTokenUseCase {

    private final RefreshTokenCredentialVerifier verifier;
    private final RefreshTokenRepository repository;
    private final TokenClockPort clock;
    private final DomainEventPublisherPort eventPublisher;

    public RevokeRefreshTokenApplicationService(
            RefreshTokenCredentialVerifier verifier,
            RefreshTokenRepository repository,
            TokenClockPort clock,
            DomainEventPublisherPort eventPublisher) {
        this.verifier = Objects.requireNonNull(verifier, "refresh token verifier must not be null");
        this.repository = Objects.requireNonNull(repository, "refresh token repository must not be null");
        this.clock = Objects.requireNonNull(clock, "token clock must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
    }

    @Override
    public RefreshTokenState revoke(RevokeRefreshTokenCommand command) {
        Objects.requireNonNull(command, "revoke refresh token command must not be null");
        RefreshToken token = verifier.verify(command.refreshToken());
        Instant now = clock.now();
        if (token.status() == TokenStatus.EXPIRED || token.expire(command.actorId(), now)) {
            repository.save(token);
            throw failure(TokenFailureReason.REFRESH_TOKEN_EXPIRED, "refresh token has expired");
        }
        if (token.status() == TokenStatus.ROTATED) {
            token.markReused(command.actorId(), now);
            Optional<RefreshToken> active = repository.findActive(token.userId(), token.sessionId());
            active.ifPresent(replacement -> {
                if (!replacement.expire(command.actorId(), now)) {
                    replacement.revoke(command.actorId(), now);
                }
            });
            repository.recordReuse(token, active);
            throw failure(TokenFailureReason.REFRESH_TOKEN_REUSED, "refresh token reuse was detected");
        }
        if (token.status() == TokenStatus.REUSED) {
            throw failure(TokenFailureReason.REFRESH_TOKEN_REUSED, "refresh token reuse was detected");
        }
        if (token.status() == TokenStatus.ACTIVE) {
            token.revoke(command.actorId(), now);
            repository.save(token);
            eventPublisher.publish(token.pullDomainEvents());
        }
        return new RefreshTokenState(token.refreshTokenId(), token.status());
    }

    private TokenApplicationException failure(TokenFailureReason reason, String message) {
        return new TokenApplicationException(reason, message);
    }
}
