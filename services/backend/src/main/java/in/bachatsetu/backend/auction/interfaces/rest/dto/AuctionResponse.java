package in.bachatsetu.backend.auction.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Safe presentation view of an auction returned by the REST API. */
public record AuctionResponse(

        @Schema(description = "Auction identifier") String auctionId,
        @Schema(description = "Owning tenant identifier") String tenantId,
        @Schema(description = "Identifier of the group the auction belongs to") String groupId,
        @Schema(description = "Identifier of the monthly cycle the auction belongs to") String cycleId,
        @Schema(description = "Auction number") int auctionNumber,
        @Schema(description = "Lifecycle status", example = "OPEN") String status,
        @Schema(description = "Timestamp the auction was scheduled to occur") Instant scheduledAt,
        @Schema(description = "Identifier of the winning member, once closed") String winnerMemberId,
        @Schema(description = "Bids submitted against this auction") List<AuctionBidResponse> bids,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Optimistic-lock version") long version) {

    public AuctionResponse {
        bids = List.copyOf(Objects.requireNonNull(bids, "bids must not be null"));
    }
}
