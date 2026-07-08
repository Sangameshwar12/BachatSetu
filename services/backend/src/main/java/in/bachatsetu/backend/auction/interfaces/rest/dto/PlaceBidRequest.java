package in.bachatsetu.backend.auction.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Request for the authenticated caller to place a discount bid against an open auction. The bidding member
 * is always the authenticated caller — not a request field — so one member cannot bid on another's behalf.
 */
public record PlaceBidRequest(

        @Positive
        @Schema(description = "Discount amount offered, in paise", example = "10000")
        long discountAmountPaise) {
}
