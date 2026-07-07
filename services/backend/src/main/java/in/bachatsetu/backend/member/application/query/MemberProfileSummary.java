package in.bachatsetu.backend.member.application.query;

import java.util.Objects;
import java.util.UUID;

/** Compact member profile view optimized for list use cases. */
public record MemberProfileSummary(
        UUID memberId,
        UUID userId,
        String memberNumber,
        String status,
        int participationCount) {

    public MemberProfileSummary {
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(memberNumber, "member number must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
