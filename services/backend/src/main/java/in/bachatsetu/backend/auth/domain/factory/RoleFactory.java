package in.bachatsetu.backend.auth.domain.factory;

import in.bachatsetu.backend.auth.domain.model.Role;
import in.bachatsetu.backend.auth.domain.model.RoleId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.util.Objects;

/** Creates roles with generated identifiers and an injected clock. */
public final class RoleFactory {

    private final Clock clock;

    public RoleFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Role create(String name, AggregateId actorId) {
        return Role.create(RoleId.newId(), name, actorId, clock.instant());
    }
}
