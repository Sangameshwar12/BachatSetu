package in.bachatsetu.backend.group.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Safe presentation view of a savings group returned by the REST API. */
public record SavingsGroupResponse(

        @Schema(description = "Group identifier") String groupId,
        @Schema(description = "Owning tenant identifier") String tenantId,
        @Schema(description = "Owning member identifier") String ownerId,
        @Schema(description = "Human-facing group code", example = "BS-1A2B3C4D5E6F7A8B") String groupCode,
        @Schema(description = "Group name") String name,
        @Schema(description = "Group description") String description,
        @Schema(description = "Group type", example = "BHISHI") String type,
        @Schema(description = "Lifecycle status", example = "INACTIVE") String status,
        @Schema(description = "Contribution amount in paise") long contributionAmountPaise,
        @Schema(description = "ISO currency code", example = "INR") String currencyCode,
        @Schema(description = "Maximum active members") int maximumMembers,
        @Schema(description = "Current active member count") int activeMemberCount,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Optimistic-lock version") long version,
        @Schema(description = "Members of this group, including removed ones") List<GroupMemberResponse> members,
        @Schema(description = "Display name of the group organizer") String organizerName) {

    public SavingsGroupResponse {
        members = List.copyOf(Objects.requireNonNull(members, "members must not be null"));
    }
}
