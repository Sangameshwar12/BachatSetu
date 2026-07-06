package in.bachatsetu.backend.group.domain.model;

import in.bachatsetu.backend.group.domain.exception.InvalidContributionAmountException;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Objects;

/** Monthly contribution amount, denominated in Indian rupees. */
public record ContributionAmount(Money value) {

    public static final long MAXIMUM_PAISE = 10_000_000L;

    public ContributionAmount {
        Objects.requireNonNull(value, "contribution amount must not be null");
        if (!Money.INR.equals(value.currency())) {
            throw new InvalidContributionAmountException("contribution amount must use INR");
        }
        if (!value.isPositive() || value.minorUnits() > MAXIMUM_PAISE) {
            throw new InvalidContributionAmountException(
                    "contribution amount must be greater than zero and no more than INR 100000");
        }
    }

    public static ContributionAmount inPaise(long paise) {
        return new ContributionAmount(Money.inr(paise));
    }
}
