package in.bachatsetu.backend.group.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable membership view returned by application queries. */
public record GroupMemberResult(
        UUID memberId,
        Instant joinedAt,
        Instant removedAt,
        boolean active) {

    public GroupMemberResult {
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(joinedAt, "joined at must not be null");
        if (active && removedAt != null || !active && removedAt == null) {
            throw new IllegalArgumentException("membership activity must match removal timestamp");
        }
    }
}
