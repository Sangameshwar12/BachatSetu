package in.bachatsetu.backend.admin.application.configuration.command;

import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Map;
import java.util.Objects;

/** Partial update: only the limit keys present in {@code changes} are modified. */
public record UpdateSystemLimitsCommand(AggregateId administratorId, Map<LimitKey, Long> changes) {

    public UpdateSystemLimitsCommand {
        Objects.requireNonNull(administratorId, "administratorId must not be null");
        changes = Map.copyOf(Objects.requireNonNull(changes, "changes must not be null"));
    }
}
