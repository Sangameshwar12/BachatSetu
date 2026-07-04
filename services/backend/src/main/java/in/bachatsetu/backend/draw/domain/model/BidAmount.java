package in.bachatsetu.backend.draw.domain.model;

import in.bachatsetu.backend.shared.domain.Money;
import java.util.Objects;

public record BidAmount(Money discount) implements Comparable<BidAmount> {

    public BidAmount {
        Objects.requireNonNull(discount, "discount must not be null");
        if (!discount.isPositive()) {
            throw new IllegalArgumentException("bid discount must be positive");
        }
    }

    @Override
    public int compareTo(BidAmount other) {
        Objects.requireNonNull(other, "other must not be null");
        return discount.compareTo(other.discount);
    }
}
