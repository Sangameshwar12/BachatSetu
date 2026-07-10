package in.bachatsetu.backend.admin.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Safe presentation view of one savings group returned by the REST API. */
public record PlatformGroupResponse(

        @Schema(description = "Group identifier") String groupId,
        @Schema(description = "Owning tenant identifier") String tenantId,
        @Schema(description = "Group code") String code,
        @Schema(description = "Group name") String name,
        @Schema(description = "Group status", example = "ACTIVE") String status,
        @Schema(description = "Number of members") int memberCount,
        @Schema(description = "Timestamp the group was created") Instant createdAt) {
}
