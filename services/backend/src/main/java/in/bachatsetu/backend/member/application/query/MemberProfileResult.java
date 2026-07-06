package in.bachatsetu.backend.member.application.query;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Complete application view of a Member profile aggregate. */
public record MemberProfileResult(
        UUID memberId,
        UUID tenantId,
        UUID userId,
        String memberNumber,
        String status,
        List<GroupParticipationResult> participations,
        List<MemberConsentResult> consents,
        long version) {

    public MemberProfileResult {
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(memberNumber, "member number must not be null");
        Objects.requireNonNull(status, "status must not be null");
        participations = List.copyOf(Objects.requireNonNull(participations, "participations must not be null"));
        consents = List.copyOf(Objects.requireNonNull(consents, "consents must not be null"));
    }
}
