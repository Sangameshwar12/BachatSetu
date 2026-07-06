package in.bachatsetu.backend.group.domain.model;

import in.bachatsetu.backend.group.domain.exception.InvalidMaximumMembersException;

/** Configured upper bound for active members in a savings group. */
public record MaximumMembers(int value) {

    public static final int MINIMUM = 2;
    public static final int MAXIMUM = 500;

    public MaximumMembers {
        if (value < MINIMUM || value > MAXIMUM) {
            throw new InvalidMaximumMembersException("maximum members must be between 2 and 500");
        }
    }

    public boolean accommodates(MemberCount count) {
        return count.value() <= value;
    }
}
