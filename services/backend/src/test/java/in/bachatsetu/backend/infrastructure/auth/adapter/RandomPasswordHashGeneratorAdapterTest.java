package in.bachatsetu.backend.infrastructure.auth.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class RandomPasswordHashGeneratorAdapterTest {

    @Test
    void generatesADistinctBcryptHashOnEveryCall() {
        RandomPasswordHashGeneratorAdapter adapter =
                new RandomPasswordHashGeneratorAdapter(new BCryptPasswordEncoder(), new SecureRandom());

        PasswordHash first = adapter.generateRandom();
        PasswordHash second = adapter.generateRandom();

        assertThat(first.value()).isNotEqualTo(second.value());
    }

    @Test
    void rejectsANullEncoder() {
        assertThatThrownBy(() -> new RandomPasswordHashGeneratorAdapter(null, new SecureRandom()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullSecureRandom() {
        assertThatThrownBy(() -> new RandomPasswordHashGeneratorAdapter(new BCryptPasswordEncoder(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
