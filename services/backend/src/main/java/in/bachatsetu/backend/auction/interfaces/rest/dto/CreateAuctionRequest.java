package in.bachatsetu.backend.auction.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request to schedule a new auction for a savings group's monthly cycle. The auction is opened immediately
 * so it can accept bids — no separate "conduct"/"open" step exists on the Auction REST surface.
 */
public record CreateAuctionRequest(

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the group the auction belongs to", example = "123e4567-e89b-12d3-a456-426614174000")
        String groupId,

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the monthly cycle the auction belongs to", example = "123e4567-e89b-12d3-a456-426614174000")
        String cycleId,

        @Positive
        @Schema(description = "Auction number; should match the referenced cycle's own cycle number", example = "1")
        int auctionNumber) {
}
