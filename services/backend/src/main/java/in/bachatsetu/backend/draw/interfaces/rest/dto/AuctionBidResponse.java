package in.bachatsetu.backend.draw.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Safe presentation view of one auction bid. */
public record AuctionBidResponse(

        @Schema(description = "Bid identifier") String bidId,
        @Schema(description = "Identifier of the bidding member") String memberId,
        @Schema(description = "Discount amount in paise") long discountAmountPaise,
        @Schema(description = "ISO currency code", example = "INR") String currencyCode,
        @Schema(description = "Timestamp the bid was submitted") Instant submittedAt,
        @Schema(description = "Bid status", example = "LEADING") String status) {
}
