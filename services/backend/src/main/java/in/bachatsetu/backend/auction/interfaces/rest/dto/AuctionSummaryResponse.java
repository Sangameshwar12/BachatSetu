package in.bachatsetu.backend.auction.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Compact presentation view of an auction returned by list endpoints. */
public record AuctionSummaryResponse(

        @Schema(description = "Auction identifier") String auctionId,
        @Schema(description = "Auction number") int auctionNumber,
        @Schema(description = "Lifecycle status", example = "OPEN") String status,
        @Schema(description = "Timestamp the auction was scheduled to occur") Instant scheduledAt,
        @Schema(description = "Identifier of the winning member, once closed") String winnerMemberId) {
}
