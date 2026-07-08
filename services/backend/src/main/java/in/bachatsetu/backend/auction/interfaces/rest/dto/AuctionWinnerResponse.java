package in.bachatsetu.backend.auction.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Safe presentation view of a closed auction's winner. */
public record AuctionWinnerResponse(

        @Schema(description = "Auction identifier") String auctionId,
        @Schema(description = "Identifier of the winning member") String memberId,
        @Schema(description = "Winning discount amount, in paise") long winningDiscountAmountPaise,
        @Schema(description = "ISO currency code", example = "INR") String currencyCode,
        @Schema(description = "Timestamp the winner was decided") Instant decidedAt) {
}
