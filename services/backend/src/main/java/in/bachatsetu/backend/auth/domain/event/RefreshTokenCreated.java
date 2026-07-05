package in.bachatsetu.backend.auth.domain.event;

import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Emitted when refresh-token lifecycle state is created. */
public record RefreshTokenCreated(
        UUID eventId,
        RefreshTokenId refreshTokenId,
        UserId userId,
        Instant expiresAt,
        Instant occurredAt) implements DomainEvent {

    public RefreshTokenCreated {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(refreshTokenId, "refresh token id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (!expiresAt.isAfter(occurredAt)) {
            throw new IllegalArgumentException("refresh token expiry must follow creation time");
        }
    }

    @Override
    public AggregateId aggregateId() {
        return refreshTokenId.toAggregateId();
    }
}
