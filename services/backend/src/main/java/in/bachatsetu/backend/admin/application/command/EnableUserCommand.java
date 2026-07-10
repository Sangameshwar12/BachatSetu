package in.bachatsetu.backend.admin.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Enables one platform user. {@code administratorId} is the acting platform administrator. */
public record EnableUserCommand(AggregateId userId, AggregateId administratorId) {

    public EnableUserCommand {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(administratorId, "administratorId must not be null");
    }
}
