package in.bachatsetu.backend.admin.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Safe presentation view of one platform user returned by the REST API. */
public record PlatformUserResponse(

        @Schema(description = "User identifier") String userId,
        @Schema(description = "Owning tenant identifier") String tenantId,
        @Schema(description = "Email address") String email,
        @Schema(description = "Phone number") String phoneNumber,
        @Schema(description = "Given name") String firstName,
        @Schema(description = "Family name") String lastName,
        @Schema(description = "Authentication status", example = "ACTIVE") String status,
        @Schema(description = "Timestamp the user was created") Instant createdAt) {
}
