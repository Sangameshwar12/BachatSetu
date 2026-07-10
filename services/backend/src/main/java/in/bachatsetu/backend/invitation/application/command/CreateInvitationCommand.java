package in.bachatsetu.backend.invitation.application.command;

import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record CreateInvitationCommand(
        AggregateId tenantId,
        AggregateId groupId,
        InvitationType type,
        AggregateId actorId) {

    public CreateInvitationCommand {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
