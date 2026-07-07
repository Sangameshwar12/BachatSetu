package in.bachatsetu.backend.draw.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Compact draw view optimized for list use cases. */
public record DrawSummary(
        UUID drawId,
        int number,
        String type,
        String status,
        Instant scheduledAt,
        UUID winnerMemberId) {

    public DrawSummary {
        Objects.requireNonNull(drawId, "draw id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(scheduledAt, "scheduled at must not be null");
    }
}
