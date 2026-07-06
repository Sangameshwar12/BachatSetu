package in.bachatsetu.backend.member.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/** Safe presentation view of a member profile returned by the REST API. */
public record MemberProfileResponse(

        @Schema(description = "Member identifier") String memberId,
        @Schema(description = "Owning tenant identifier") String tenantId,
        @Schema(description = "Identifier of the underlying user") String userId,
        @Schema(description = "Human-facing member number", example = "MB-1A2B3C4D5E6F7A8B") String memberNumber,
        @Schema(description = "Lifecycle status", example = "INVITED") String status,
        @Schema(description = "Group participations held by this member") List<GroupParticipationResponse> participations,
        @Schema(description = "Optimistic-lock version") long version) {

    public MemberProfileResponse {
        participations = List.copyOf(Objects.requireNonNull(participations, "participations must not be null"));
    }
}
