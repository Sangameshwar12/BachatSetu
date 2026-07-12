package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Request to rotate an opaque refresh credential for a new access/refresh token pair. */
public record RefreshTokenRequest(
        @NotBlank
        @Schema(description = "Opaque refresh token in id.secret form", format = "password")
        String refreshToken) {
}
