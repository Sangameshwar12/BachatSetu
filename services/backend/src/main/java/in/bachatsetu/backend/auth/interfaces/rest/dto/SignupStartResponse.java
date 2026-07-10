package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Confirms an account was created and an OTP was dispatched. */
public record SignupStartResponse(
        @Schema(description = "Newly created user identifier", example = "123e4567-e89b-12d3-a456-426614174000")
        String userId,

        @Schema(description = "Mobile number the OTP was sent to", example = "+919876543210")
        String mobileNumber,

        @Schema(description = "When the dispatched OTP expires")
        Instant otpExpiresAt) {
}
