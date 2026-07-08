package in.bachatsetu.backend.auction.application.command;

import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/**
 * Requests scheduling of a new auction for a savings group's monthly cycle. Carries no
 * {@code scheduledAt}: the auction is always opened immediately (see
 * {@code CreateAuctionApplicationService}), so there is no caller-supplied schedule to accept.
 */
public record CreateAuctionCommand(
        AggregateId tenantId,
        AggregateId groupId,
        AggregateId cycleId,
        DrawNumber number,
        AggregateId actorId) {

    public CreateAuctionCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(cycleId, "cycle id must not be null");
        Objects.requireNonNull(number, "auction number must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
