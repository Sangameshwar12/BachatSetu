package in.bachatsetu.backend.dashboard.application.query;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public record NextDrawSummary(AggregateId drawId, Instant scheduledAt, String status) {

    public NextDrawSummary {
        Objects.requireNonNull(drawId, "drawId must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
