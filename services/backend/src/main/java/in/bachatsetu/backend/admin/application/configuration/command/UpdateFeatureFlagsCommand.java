package in.bachatsetu.backend.admin.application.configuration.command;

import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Map;
import java.util.Objects;

/** Partial update: only the feature keys present in {@code changes} are modified. */
public record UpdateFeatureFlagsCommand(AggregateId administratorId, Map<FeatureKey, Boolean> changes) {

    public UpdateFeatureFlagsCommand {
        Objects.requireNonNull(administratorId, "administratorId must not be null");
        changes = Map.copyOf(Objects.requireNonNull(changes, "changes must not be null"));
    }
}
