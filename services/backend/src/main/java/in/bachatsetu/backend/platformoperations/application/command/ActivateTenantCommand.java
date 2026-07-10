package in.bachatsetu.backend.platformoperations.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record ActivateTenantCommand(AggregateId tenantId, AggregateId actorId) {

    public ActivateTenantCommand {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
