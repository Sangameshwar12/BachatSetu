package in.bachatsetu.backend.draw.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Safe presentation view of a draw returned by the REST API. */
public record DrawResponse(

        @Schema(description = "Draw identifier") String drawId,
        @Schema(description = "Owning tenant identifier") String tenantId,
        @Schema(description = "Identifier of the group the draw belongs to") String groupId,
        @Schema(description = "Identifier of the monthly cycle the draw belongs to") String cycleId,
        @Schema(description = "Draw number") int drawNumber,
        @Schema(description = "Draw type", example = "AUCTION") String type,
        @Schema(description = "Lifecycle status", example = "SCHEDULED") String status,
        @Schema(description = "Timestamp the draw is scheduled to occur") Instant scheduledAt,
        @Schema(description = "Identifier of the winning member, once completed") String winnerMemberId,
        @Schema(description = "Auction bids submitted against this draw") List<AuctionBidResponse> bids,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Optimistic-lock version") long version) {

    public DrawResponse {
        bids = List.copyOf(Objects.requireNonNull(bids, "bids must not be null"));
    }
}
