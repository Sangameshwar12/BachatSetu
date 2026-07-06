package in.bachatsetu.backend.group.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/**
 * A member's identity and membership history inside a savings group aggregate.
 * Identity equality is based on the member identifier.
 */
public final class GroupMember {

    private final AggregateId memberId;
    private final CreatedAt joinedAt;
    private final UpdatedAt removedAt;

    private GroupMember(AggregateId memberId, CreatedAt joinedAt, UpdatedAt removedAt) {
        this.memberId = Objects.requireNonNull(memberId, "member id must not be null");
        this.joinedAt = Objects.requireNonNull(joinedAt, "joined at must not be null");
        this.removedAt = removedAt;
        if (removedAt != null && removedAt.value().isBefore(joinedAt.value())) {
            throw new IllegalArgumentException("removed at must not precede joined at");
        }
    }

    public static GroupMember join(AggregateId memberId, CreatedAt joinedAt) {
        return new GroupMember(memberId, joinedAt, null);
    }

    public static GroupMember rehydrate(AggregateId memberId, CreatedAt joinedAt, UpdatedAt removedAt) {
        return new GroupMember(memberId, joinedAt, removedAt);
    }

    public GroupMember remove(UpdatedAt removedAt) {
        Objects.requireNonNull(removedAt, "removed at must not be null");
        if (!isActive()) {
            throw new IllegalStateException("member has already been removed");
        }
        return new GroupMember(memberId, joinedAt, removedAt);
    }

    public AggregateId memberId() {
        return memberId;
    }

    public CreatedAt joinedAt() {
        return joinedAt;
    }

    public UpdatedAt removedAt() {
        return removedAt;
    }

    public boolean isActive() {
        return removedAt == null;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof GroupMember member && memberId.equals(member.memberId);
    }

    @Override
    public int hashCode() {
        return memberId.hashCode();
    }
}
