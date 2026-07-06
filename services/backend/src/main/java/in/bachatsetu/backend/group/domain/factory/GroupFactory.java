package in.bachatsetu.backend.group.domain.factory;

import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupRule;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.util.Objects;

/** Legacy explicit-code factory retained for callers that already supply group identifiers. */
public final class GroupFactory {

    private final Clock clock;

    public GroupFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public SavingsGroup create(
            AggregateId tenantId,
            AggregateId organizerId,
            GroupCode code,
            GroupName name,
            GroupType type,
            GroupRule rule) {
        return SavingsGroup.create(
                AggregateId.newId(), tenantId, organizerId, code, name, type, rule, clock.instant());
    }
}
