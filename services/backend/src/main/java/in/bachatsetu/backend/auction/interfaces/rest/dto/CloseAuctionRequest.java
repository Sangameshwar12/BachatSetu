package in.bachatsetu.backend.auction.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request to close an open auction with its winning member. */
public record CloseAuctionRequest(

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the winning member", example = "123e4567-e89b-12d3-a456-426614174000")
        String winnerId) {
}
