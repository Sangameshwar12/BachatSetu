package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request to verify an active OTP challenge. */
public record OtpVerifyRequest(
        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Authentication user identifier", example = "123e4567-e89b-12d3-a456-426614174000")
        String userId,

        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = ValidationPatterns.OTP_PURPOSE, message = "must be a supported OTP purpose")
        @Schema(description = "Business purpose for the OTP", example = "SIGN_IN")
        String purpose,

        @NotBlank
        @Size(min = 6, max = 6)
        @Pattern(regexp = ValidationPatterns.OTP_CODE, message = "must contain exactly six digits")
        @Schema(description = "Six-digit OTP supplied by the user", example = "482913", format = "password")
        String code) {
}
