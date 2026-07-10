package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request to start self-registration. */
public record SignupStartRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "Given name", example = "Asha")
        String givenName,

        @Size(max = 100)
        @Schema(description = "Family name", example = "Rao")
        String familyName,

        @NotBlank
        @Pattern(regexp = "^\\+91[6-9]\\d{9}$", message = "must be an Indian mobile number in +91 format")
        @Schema(description = "Indian mobile number in +91 format", example = "+919876543210")
        String mobileNumber,

        @Email
        @Size(max = 254)
        @Schema(description = "Optional email address", example = "asha@example.in")
        String email,

        @NotBlank
        @Pattern(regexp = "ENGLISH|HINDI|MARATHI", message = "must be a supported preferred language")
        @Schema(description = "Preferred language", example = "ENGLISH")
        String preferredLanguage,

        @AssertTrue(message = "terms and conditions must be accepted")
        @Schema(description = "Whether the caller accepted the terms and conditions", example = "true")
        boolean acceptedTerms) {
}
