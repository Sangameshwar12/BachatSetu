package in.bachatsetu.backend.platformoperations.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record SuspendTenantCommand(AggregateId tenantId, String reason, AggregateId actorId) {

    public SuspendTenantCommand {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
