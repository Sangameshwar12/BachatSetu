package in.bachatsetu.backend.member.domain.model;

import in.bachatsetu.backend.member.domain.exception.InvalidMembershipStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public final class GroupParticipation {

    private final AggregateId id;
    private final AggregateId groupId;
    private final GroupRole role;
    private final Instant joinedAt;
    private ParticipationStatus status;
    private Instant exitedAt;

    public GroupParticipation(
            AggregateId id,
            AggregateId groupId,
            GroupRole role,
            Instant joinedAt,
            ParticipationStatus status,
            Instant exitedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.joinedAt = Objects.requireNonNull(joinedAt, "joinedAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.exitedAt = exitedAt;
    }

    public void activate() {
        if (status != ParticipationStatus.INVITED && status != ParticipationStatus.PAUSED) {
            throw new InvalidMembershipStateException("participation cannot be activated");
        }
        status = ParticipationStatus.ACTIVE;
    }

    public void exit(Instant exitTime) {
        Objects.requireNonNull(exitTime, "exitTime must not be null");
        if (status != ParticipationStatus.ACTIVE || exitTime.isBefore(joinedAt)) {
            throw new InvalidMembershipStateException("participation cannot be exited");
        }
        status = ParticipationStatus.EXITED;
        exitedAt = exitTime;
    }

    public AggregateId id() {
        return id;
    }

    public AggregateId groupId() {
        return groupId;
    }

    public GroupRole role() {
        return role;
    }

    public Instant joinedAt() {
        return joinedAt;
    }

    public ParticipationStatus status() {
        return status;
    }

    public Instant exitedAt() {
        return exitedAt;
    }
}
