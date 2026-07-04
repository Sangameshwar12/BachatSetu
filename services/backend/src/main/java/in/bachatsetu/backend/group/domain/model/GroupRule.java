package in.bachatsetu.backend.group.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public final class GroupRule {

    private final AggregateId id;
    private final ContributionSchedule contributionSchedule;
    private final MemberCapacity memberCapacity;
    private final PayoutMethod payoutMethod;
    private final boolean partialPaymentsAllowed;

    public GroupRule(
            AggregateId id,
            ContributionSchedule contributionSchedule,
            MemberCapacity memberCapacity,
            PayoutMethod payoutMethod,
            boolean partialPaymentsAllowed) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.contributionSchedule = Objects.requireNonNull(
                contributionSchedule, "contributionSchedule must not be null");
        this.memberCapacity = Objects.requireNonNull(memberCapacity, "memberCapacity must not be null");
        this.payoutMethod = Objects.requireNonNull(payoutMethod, "payoutMethod must not be null");
        this.partialPaymentsAllowed = partialPaymentsAllowed;
    }

    public AggregateId id() {
        return id;
    }

    public ContributionSchedule contributionSchedule() {
        return contributionSchedule;
    }

    public MemberCapacity memberCapacity() {
        return memberCapacity;
    }

    public PayoutMethod payoutMethod() {
        return payoutMethod;
    }

    public boolean partialPaymentsAllowed() {
        return partialPaymentsAllowed;
    }
}
