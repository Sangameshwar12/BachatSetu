package in.bachatsetu.backend.invitation.application.command;

import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Accepts an invitation identified by exactly one of {@code code} or {@code token}. */
public record AcceptInvitationCommand(
        AggregateId tenantId,
        String code,
        String token,
        InvitationType channel,
        AggregateId actorId) {

    public AcceptInvitationCommand {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        boolean hasCode = code != null && !code.isBlank();
        boolean hasToken = token != null && !token.isBlank();
        if (hasCode == hasToken) {
            throw new IllegalArgumentException("exactly one of code or token must be supplied");
        }
    }
}
