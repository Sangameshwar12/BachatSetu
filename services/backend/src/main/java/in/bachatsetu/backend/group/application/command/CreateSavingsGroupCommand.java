package in.bachatsetu.backend.group.application.command;

import in.bachatsetu.backend.group.domain.model.GroupDescription;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupRule;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests creation of a new Savings Group aggregate. */
public record CreateSavingsGroupCommand(
        AggregateId tenantId,
        OwnerId ownerId,
        GroupName name,
        GroupDescription description,
        GroupType type,
        GroupRule rule) {

    public CreateSavingsGroupCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(ownerId, "owner id must not be null");
        Objects.requireNonNull(name, "group name must not be null");
        Objects.requireNonNull(description, "group description must not be null");
        Objects.requireNonNull(type, "group type must not be null");
        Objects.requireNonNull(rule, "group rule must not be null");
    }
}
