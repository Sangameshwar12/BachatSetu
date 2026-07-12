package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Request to revoke a refresh token and end its session. */
public record LogoutRequest(
        @NotBlank
        @Schema(description = "Opaque refresh token in id.secret form", format = "password")
        String refreshToken) {
}
