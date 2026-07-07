package in.bachatsetu.backend.draw.application.query;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Complete application view of a Draw aggregate. */
public record DrawResult(
        UUID drawId,
        UUID tenantId,
        UUID groupId,
        UUID cycleId,
        int number,
        String type,
        String status,
        Instant scheduledAt,
        UUID winnerMemberId,
        List<AuctionBidResult> bids,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public DrawResult {
        Objects.requireNonNull(drawId, "draw id must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(cycleId, "cycle id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(scheduledAt, "scheduled at must not be null");
        bids = List.copyOf(Objects.requireNonNull(bids, "bids must not be null"));
        Objects.requireNonNull(createdAt, "created at must not be null");
        Objects.requireNonNull(updatedAt, "updated at must not be null");
    }
}
