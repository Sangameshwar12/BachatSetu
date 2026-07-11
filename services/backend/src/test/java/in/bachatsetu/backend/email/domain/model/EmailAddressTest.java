package in.bachatsetu.backend.email.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailAddressTest {

    @Test
    void acceptsAWellFormedAddress() {
        assertThat(new EmailAddress("user@example.com").value()).isEqualTo("user@example.com");
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(new EmailAddress("  user@example.com  ").value()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsAnAddressWithNoAtSign() {
        assertThatThrownBy(() -> new EmailAddress("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnAddressStartingWithAtSign() {
        assertThatThrownBy(() -> new EmailAddress("@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnAddressEndingWithAtSign() {
        assertThatThrownBy(() -> new EmailAddress("user@"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankAddress() {
        assertThatThrownBy(() -> new EmailAddress("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullAddress() {
        assertThatThrownBy(() -> new EmailAddress(null))
                .isInstanceOf(NullPointerException.class);
    }
}
