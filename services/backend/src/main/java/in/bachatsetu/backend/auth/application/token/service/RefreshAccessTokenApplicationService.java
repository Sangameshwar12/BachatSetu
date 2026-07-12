package in.bachatsetu.backend.auth.application.token.service;

import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auth.application.token.command.RefreshAccessTokenCommand;
import in.bachatsetu.backend.auth.application.token.exception.TokenApplicationException;
import in.bachatsetu.backend.auth.application.token.exception.TokenFailureReason;
import in.bachatsetu.backend.auth.application.token.port.IssuedAccessToken;
import in.bachatsetu.backend.auth.application.token.port.IssuedRefreshToken;
import in.bachatsetu.backend.auth.application.token.port.JwtProviderPort;
import in.bachatsetu.backend.auth.application.token.port.TokenClockPort;
import in.bachatsetu.backend.auth.application.token.port.TokenHasherPort;
import in.bachatsetu.backend.auth.application.token.query.RefreshTokenResult;
import in.bachatsetu.backend.auth.application.token.query.TokenPairResult;
import in.bachatsetu.backend.auth.application.token.usecase.RefreshAccessTokenUseCase;
import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.TokenStatus;
import in.bachatsetu.backend.auth.domain.port.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RefreshAccessTokenApplicationService implements RefreshAccessTokenUseCase {

    private final RefreshTokenCredentialVerifier verifier;
    private final RefreshTokenRepository repository;
    private final TokenPrincipalResolver principals;
    private final JwtProviderPort jwtProvider;
    private final TokenHasherPort hasher;
    private final TokenClockPort clock;
    private final Duration lifetime;
    private final DomainEventPublisherPort eventPublisher;

    public RefreshAccessTokenApplicationService(
            RefreshTokenCredentialVerifier verifier,
            RefreshTokenRepository repository,
            TokenPrincipalResolver principals,
            JwtProviderPort jwtProvider,
            TokenHasherPort hasher,
            TokenClockPort clock,
            Duration lifetime,
            DomainEventPublisherPort eventPublisher) {
        this.verifier = Objects.requireNonNull(verifier, "refresh token verifier must not be null");
        this.repository = Objects.requireNonNull(repository, "refresh token repository must not be null");
        this.principals = Objects.requireNonNull(principals, "token principal resolver must not be null");
        this.jwtProvider = Objects.requireNonNull(jwtProvider, "JWT provider must not be null");
        this.hasher = Objects.requireNonNull(hasher, "token hasher must not be null");
        this.clock = Objects.requireNonNull(clock, "token clock must not be null");
        this.lifetime = requirePositive(lifetime);
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
    }

    @Override
    public TokenPairResult refresh(RefreshAccessTokenCommand command) {
        Objects.requireNonNull(command, "refresh access token command must not be null");
        RefreshToken current = verifier.verify(command.refreshToken());
        Instant now = clock.now();
        rejectTerminalOrReused(current, command.actorId(), now);
        if (current.expire(command.actorId(), now)) {
            repository.save(current);
            throw failure(TokenFailureReason.REFRESH_TOKEN_EXPIRED, "refresh token has expired");
        }
        IssuedAccessToken accessToken = jwtProvider.issue(
                principals.resolve(current.userId(), current.tenantId()));
        RefreshTokenId replacementId = RefreshTokenId.newId();
        IssuedRefreshToken issued = hasher.issue(replacementId);
        RefreshToken replacement = RefreshToken.issue(
                replacementId,
                current.userId(),
                current.tenantId(),
                current.sessionId(),
                issued.hash(),
                now,
                now.plus(lifetime),
                command.actorId());
        current.rotate(replacementId, command.actorId(), now);
        repository.replace(current, replacement);
        eventPublisher.publish(replacement.pullDomainEvents());
        return new TokenPairResult(
                accessToken,
                new RefreshTokenResult(issued.credential(), replacement.sessionId(), replacement.expiresAt()));
    }

    private void rejectTerminalOrReused(RefreshToken token, in.bachatsetu.backend.shared.domain.AggregateId actorId, Instant now) {
        if (token.status() == TokenStatus.ROTATED) {
            token.markReused(actorId, now);
            Optional<RefreshToken> active = repository.findActive(token.userId(), token.sessionId());
            active.ifPresent(replacement -> revokeOrExpire(replacement, actorId, now));
            repository.recordReuse(token, active);
            throw failure(TokenFailureReason.REFRESH_TOKEN_REUSED, "refresh token reuse was detected");
        }
        if (token.status() == TokenStatus.REUSED) {
            throw failure(TokenFailureReason.REFRESH_TOKEN_REUSED, "refresh token reuse was detected");
        }
        if (token.status() == TokenStatus.REVOKED) {
            throw failure(TokenFailureReason.REFRESH_TOKEN_REVOKED, "refresh token has been revoked");
        }
        if (token.status() == TokenStatus.EXPIRED) {
            throw failure(TokenFailureReason.REFRESH_TOKEN_EXPIRED, "refresh token has expired");
        }
    }

    private void revokeOrExpire(
            RefreshToken token,
            in.bachatsetu.backend.shared.domain.AggregateId actorId,
            Instant now) {
        if (!token.expire(actorId, now)) {
            token.revoke(actorId, now);
        }
    }

    private static Duration requirePositive(Duration value) {
        Objects.requireNonNull(value, "refresh token lifetime must not be null");
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException("refresh token lifetime must be positive");
        }
        return value;
    }

    private TokenApplicationException failure(TokenFailureReason reason, String message) {
        return new TokenApplicationException(reason, message);
    }
}
