package in.bachatsetu.backend.admin.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/** The outcome of one {@link PlatformAdministration} enable/disable decision, ready to be persisted. */
public record PlatformUserStatusChange(
        AggregateId userId, PlatformUserStatus targetStatus, AggregateId administratorId, Instant changedAt) {

    public PlatformUserStatusChange {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        Objects.requireNonNull(administratorId, "administratorId must not be null");
        Objects.requireNonNull(changedAt, "changedAt must not be null");
    }
}
