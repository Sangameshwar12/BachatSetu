package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** The caller's first access and refresh tokens after a successful signup. */
public record SignupVerifyResponse(
        @Schema(description = "Activated user identifier", example = "123e4567-e89b-12d3-a456-426614174000")
        String userId,

        @Schema(description = "Bearer access token") String accessToken,
        @Schema(description = "Access token expiry") Instant accessTokenExpiresAt,
        @Schema(description = "Opaque refresh token") String refreshToken,
        @Schema(description = "Refresh token expiry") Instant refreshTokenExpiresAt,
        @Schema(description = "Token type", example = "Bearer") String tokenType) {
}
