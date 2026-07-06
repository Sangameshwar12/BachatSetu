package in.bachatsetu.backend.group.domain.model;

/** Minimum and maximum participation bounds configured for a group rule. */
public record MemberCapacity(int minimum, int maximum) {

    public MemberCapacity {
        if (minimum < 2) {
            throw new IllegalArgumentException("minimum member capacity must be at least 2");
        }
        if (maximum < minimum || maximum > MaximumMembers.MAXIMUM) {
            throw new IllegalArgumentException("maximum member capacity is invalid");
        }
    }

    public boolean supports(int memberCount) {
        return memberCount >= minimum && memberCount <= maximum;
    }
}
