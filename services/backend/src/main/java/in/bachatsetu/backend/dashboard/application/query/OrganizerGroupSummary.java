package in.bachatsetu.backend.dashboard.application.query;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/**
 * @param contributionProgressPercent share of active members with at least one verified payment
 *     toward this group, 0-100. A simplified real proxy for "contribution progress": the codebase
 *     has no per-cycle installment schedule to compare against (see docs/application/group-invitations.md).
 */
public record OrganizerGroupSummary(
        AggregateId groupId,
        String groupCode,
        String name,
        int memberCount,
        int maximumMembers,
        boolean hasActiveInvitation,
        NextDrawSummary nextDraw,
        int contributionProgressPercent) {

    public OrganizerGroupSummary {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(groupCode, "groupCode must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (contributionProgressPercent < 0 || contributionProgressPercent > 100) {
            throw new IllegalArgumentException("contributionProgressPercent must be between 0 and 100");
        }
    }
}
