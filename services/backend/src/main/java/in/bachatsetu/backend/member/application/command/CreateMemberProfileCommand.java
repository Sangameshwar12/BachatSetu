package in.bachatsetu.backend.member.application.command;

import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests creation of a new Member profile together with its first group participation. */
public record CreateMemberProfileCommand(
        AggregateId tenantId,
        AggregateId userId,
        AggregateId groupId,
        GroupRole role,
        AggregateId actorId) {

    public CreateMemberProfileCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
