package in.bachatsetu.backend.payment.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Safe presentation view of one payment attempt. */
public record PaymentAttemptResponse(

        @Schema(description = "Attempt identifier") String attemptId,
        @Schema(description = "Attempt sequence number") int sequence,
        @Schema(description = "Timestamp the attempt was initiated") Instant initiatedAt,
        @Schema(description = "Attempt status", example = "SUCCEEDED") String status,
        @Schema(description = "Payment provider name, if recorded") String provider,
        @Schema(description = "Provider transaction identifier, if recorded") String transactionId,
        @Schema(description = "Failure code, if the attempt failed") String failureCode) {
}
