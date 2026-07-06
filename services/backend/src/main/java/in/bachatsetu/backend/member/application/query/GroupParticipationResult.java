package in.bachatsetu.backend.member.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Safe application view of one group participation. */
public record GroupParticipationResult(
        UUID groupId,
        String role,
        Instant joinedAt,
        Instant exitedAt,
        String status) {

    public GroupParticipationResult {
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(joinedAt, "joined at must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
