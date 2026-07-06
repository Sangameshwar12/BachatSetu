package in.bachatsetu.backend.group.application.command;

import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests suspension of an active savings group. */
public record SuspendGroupCommand(AggregateId tenantId, GroupId groupId, AggregateId actorId) {

    public SuspendGroupCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
