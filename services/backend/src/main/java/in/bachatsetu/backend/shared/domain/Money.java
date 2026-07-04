package in.bachatsetu.backend.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(long minorUnits, Currency currency) implements Comparable<Money> {

    public static final Currency INR = Currency.getInstance("INR");

    public Money {
        Objects.requireNonNull(currency, "currency must not be null");
    }

    public static Money inr(long paise) {
        return new Money(paise, INR);
    }

    public static Money zero(Currency currency) {
        return new Money(0, currency);
    }

    public static Money ofMajor(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        int fractionDigits = currency.getDefaultFractionDigits();
        long minorUnits = amount.movePointRight(fractionDigits)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
        return new Money(minorUnits, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(minorUnits, other.minorUnits), currency);
    }

    public Money multiply(long multiplier) {
        return new Money(Math.multiplyExact(minorUnits, multiplier), currency);
    }

    public boolean isNegative() {
        return minorUnits < 0;
    }

    public boolean isPositive() {
        return minorUnits > 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(minorUnits, other.minorUnits);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("money currencies must match");
        }
    }
}
