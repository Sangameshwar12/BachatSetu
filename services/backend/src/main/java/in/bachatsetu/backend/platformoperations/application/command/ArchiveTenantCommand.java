package in.bachatsetu.backend.platformoperations.application.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record ArchiveTenantCommand(AggregateId tenantId, AggregateId actorId) {

    public ArchiveTenantCommand {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
