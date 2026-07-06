package in.bachatsetu.backend.group.domain.model;

/** Number of members currently participating in a savings group. */
public record MemberCount(int value) {

    public MemberCount {
        if (value < 0) {
            throw new IllegalArgumentException("member count must not be negative");
        }
    }

    public MemberCount increment() {
        return new MemberCount(Math.incrementExact(value));
    }

    public MemberCount decrement() {
        if (value == 0) {
            throw new IllegalStateException("member count cannot be decremented below zero");
        }
        return new MemberCount(value - 1);
    }
}
