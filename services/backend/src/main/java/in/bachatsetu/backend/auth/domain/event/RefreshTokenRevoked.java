package in.bachatsetu.backend.auth.domain.event;

import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Emitted when a refresh-token record is revoked. */
public record RefreshTokenRevoked(
        UUID eventId,
        RefreshTokenId refreshTokenId,
        UserId userId,
        Instant occurredAt) implements DomainEvent {

    public RefreshTokenRevoked {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(refreshTokenId, "refresh token id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    @Override
    public AggregateId aggregateId() {
        return refreshTokenId.toAggregateId();
    }
}
