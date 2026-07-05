package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Successful OTP verification response. */
public record OtpVerifyResponse(
        @Schema(example = "9b7ee98d-4935-4d24-aafa-58048e559f1d") String verificationId,
        @Schema(example = "VERIFIED") String status,
        @Schema(example = "true") boolean verified,
        @Schema(example = "1") int verificationAttempts) {
}
