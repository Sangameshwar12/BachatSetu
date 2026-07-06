package in.bachatsetu.backend.group.application.command;

import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests activation of an inactive or suspended savings group. */
public record ActivateGroupCommand(AggregateId tenantId, GroupId groupId, AggregateId actorId) {

    public ActivateGroupCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
