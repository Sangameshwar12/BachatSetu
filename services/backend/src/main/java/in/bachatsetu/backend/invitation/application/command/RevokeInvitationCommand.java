package in.bachatsetu.backend.invitation.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record RevokeInvitationCommand(AggregateId tenantId, AggregateId groupId, AggregateId actorId) {

    public RevokeInvitationCommand {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
