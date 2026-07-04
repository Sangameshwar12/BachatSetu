package in.bachatsetu.backend.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void performsExactMinorUnitArithmetic() {
        Money total = Money.inr(10_000).add(Money.inr(2_500)).subtract(Money.inr(500));

        assertThat(total).isEqualTo(Money.inr(12_000));
    }

    @Test
    void rejectsArithmeticAcrossCurrencies() {
        Money rupees = Money.inr(100);
        Money dollars = new Money(100, Currency.getInstance("USD"));

        assertThatIllegalArgumentException().isThrownBy(() -> rupees.add(dollars));
    }
}
