package in.bachatsetu.backend.payment.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** One group member's contribution status for the current cycle. */
public record MemberCollectionResponse(

        @Schema(description = "Member identifier") String memberId,
        @Schema(description = "Contribution status for the current cycle", example = "PAID") String status,
        @Schema(description = "Expected contribution amount in paise") long expectedAmountPaise,
        @Schema(description = "Amount actually collected from this member this cycle, in paise")
                long collectedAmountPaise,
        @Schema(description = "When this member's contribution was verified, if paid") Instant paidAt,
        @Schema(description = "Date this cycle's contribution is due") LocalDate dueDate) {
}
