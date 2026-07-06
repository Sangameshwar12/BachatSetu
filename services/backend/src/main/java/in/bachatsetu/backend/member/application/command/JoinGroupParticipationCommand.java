package in.bachatsetu.backend.member.application.command;

import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests that an existing Member profile join an additional group. */
public record JoinGroupParticipationCommand(
        AggregateId tenantId,
        AggregateId memberId,
        AggregateId groupId,
        GroupRole role,
        AggregateId actorId) {

    public JoinGroupParticipationCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
