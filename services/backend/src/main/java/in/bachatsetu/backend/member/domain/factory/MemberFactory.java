package in.bachatsetu.backend.member.domain.factory;

import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.util.Objects;

public final class MemberFactory {

    private final Clock clock;

    public MemberFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public MemberProfile create(
            AggregateId tenantId,
            AggregateId userId,
            MemberNumber memberNumber,
            AggregateId actorId) {
        return MemberProfile.create(
                AggregateId.newId(), tenantId, userId, memberNumber, actorId, clock.instant());
    }
}
