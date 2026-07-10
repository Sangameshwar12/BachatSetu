package in.bachatsetu.backend.admin.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Disables one platform user. {@code administratorId} is the acting platform administrator. */
public record DisableUserCommand(AggregateId userId, AggregateId administratorId) {

    public DisableUserCommand {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(administratorId, "administratorId must not be null");
    }
}
