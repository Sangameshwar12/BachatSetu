package in.bachatsetu.backend.group.application.command;

import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests that a member join an existing savings group. */
public record JoinGroupCommand(
        AggregateId tenantId,
        GroupId groupId,
        AggregateId memberId,
        AggregateId actorId) {

    public JoinGroupCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
