package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request to start a returning-user login. */
public record LoginStartRequest(
        @NotBlank
        @Pattern(regexp = "^\\+91[6-9]\\d{9}$", message = "must be an Indian mobile number in +91 format")
        @Schema(description = "Indian mobile number in +91 format", example = "+919876543210")
        String mobileNumber) {
}
