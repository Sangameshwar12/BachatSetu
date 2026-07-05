package in.bachatsetu.backend.auth.domain.factory;

import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenHash;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Creates refresh-token lifecycle aggregates using a configured positive lifetime. */
public final class RefreshTokenFactory {

    private final Clock clock;
    private final Duration lifetime;

    public RefreshTokenFactory(Clock clock, Duration lifetime) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.lifetime = requirePositive(lifetime);
    }

    public RefreshToken issue(
            UserId userId,
            AggregateId tenantId,
            TokenSessionId sessionId,
            RefreshTokenHash tokenHash,
            AggregateId actorId) {
        Instant issuedAt = clock.instant();
        return RefreshToken.issue(
                RefreshTokenId.newId(),
                userId,
                tenantId,
                sessionId,
                tokenHash,
                issuedAt,
                issuedAt.plus(lifetime),
                actorId);
    }

    private static Duration requirePositive(Duration value) {
        Objects.requireNonNull(value, "refresh token lifetime must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("refresh token lifetime must be positive");
        }
        return value;
    }
}
