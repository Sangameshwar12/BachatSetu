package in.bachatsetu.backend.member.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Compact presentation view of a member profile returned by list endpoints. */
public record MemberProfileSummaryResponse(

        @Schema(description = "Member identifier") String memberId,
        @Schema(description = "Identifier of the underlying user") String userId,
        @Schema(description = "Human-facing member number", example = "MB-1A2B3C4D5E6F7A8B") String memberNumber,
        @Schema(description = "Lifecycle status", example = "ACTIVE") String status,
        @Schema(description = "Number of group participations held by this member") int participationCount) {
}
