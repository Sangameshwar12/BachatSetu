package in.bachatsetu.backend.draw.domain.factory;

import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class DrawFactory {

    private final Clock clock;

    public DrawFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Draw schedule(
            AggregateId tenantId,
            AggregateId groupId,
            AggregateId cycleId,
            DrawNumber number,
            DrawType type,
            Instant scheduledAt,
            AggregateId actorId) {
        return Draw.schedule(
                AggregateId.newId(),
                tenantId,
                groupId,
                cycleId,
                number,
                type,
                scheduledAt,
                actorId,
                clock.instant());
    }
}
