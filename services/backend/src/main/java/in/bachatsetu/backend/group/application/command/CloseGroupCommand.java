package in.bachatsetu.backend.group.application.command;

import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests permanent closure of an active or inactive savings group. */
public record CloseGroupCommand(AggregateId tenantId, GroupId groupId, AggregateId actorId) {

    public CloseGroupCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
