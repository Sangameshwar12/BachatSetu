package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Public OTP challenge state without credential material. */
public record OtpRequestResponse(
        @Schema(example = "9b7ee98d-4935-4d24-aafa-58048e559f1d") String verificationId,
        @Schema(example = "SIGN_IN") String purpose,
        @Schema(example = "PENDING") String status,
        @Schema(example = "2026-07-05T10:05:00Z") Instant expiresAt,
        @Schema(example = "0") int resendCount) {
}
