package in.bachatsetu.backend.auth.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** A newly rotated access and refresh token pair. */
public record RefreshTokenResponse(
        @Schema(description = "User identifier", example = "123e4567-e89b-12d3-a456-426614174000")
        String userId,

        @Schema(description = "Bearer access token") String accessToken,
        @Schema(description = "Access token expiry") Instant accessTokenExpiresAt,
        @Schema(description = "New opaque refresh token") String refreshToken,
        @Schema(description = "Refresh token expiry") Instant refreshTokenExpiresAt,
        @Schema(description = "Token type", example = "Bearer") String tokenType) {
}
