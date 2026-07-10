package in.bachatsetu.backend.invitation.application.query;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public record InvitationAcceptedResult(AggregateId groupId, AggregateId memberId, Instant joinedAt) {

    public InvitationAcceptedResult {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(joinedAt, "joinedAt must not be null");
    }
}
