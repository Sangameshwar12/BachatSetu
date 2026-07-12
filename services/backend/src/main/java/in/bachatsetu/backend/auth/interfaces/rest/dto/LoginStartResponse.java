package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Confirms a sign-in OTP was dispatched to the given mobile number. */
public record LoginStartResponse(
        @Schema(description = "User identifier", example = "123e4567-e89b-12d3-a456-426614174000")
        String userId,

        @Schema(description = "Mobile number the OTP was sent to", example = "+919876543210")
        String mobileNumber,

        @Schema(description = "When the dispatched OTP expires")
        Instant otpExpiresAt) {
}
