package in.bachatsetu.backend.platformoperations.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record BroadcastRecipient(AggregateId tenantId, AggregateId userId) {

    public BroadcastRecipient {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
    }
}
