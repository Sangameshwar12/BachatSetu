package in.bachatsetu.backend.infrastructure.auth.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import in.bachatsetu.backend.auth.application.token.port.RefreshTokenCredential;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenHash;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class TokenSecurityAdapterTest {

    @Test
    void generatesUniqueOpaqueCredentialsAndVerifiesOnlyMatchingHash() {
        var adapter = new BCryptTokenHasherAdapter(
                new SecureRandom(), new BCryptPasswordEncoder(4));
        RefreshTokenId tokenId = RefreshTokenId.newId();
        var first = adapter.issue(tokenId);
        var second = adapter.issue(RefreshTokenId.newId());

        assertThat(first.credential().value()).doesNotContain(first.hash().value());
        assertThat(first.credential().value()).isNotEqualTo(second.credential().value());
        assertThat(adapter.matches(first.credential(), first.hash())).isTrue();
        assertThat(adapter.matches(second.credential(), first.hash())).isFalse();
    }

    @Test
    void rejectsMissingHasherInputs() {
        var adapter = new BCryptTokenHasherAdapter(
                new SecureRandom(), new BCryptPasswordEncoder(4));

        assertThatNullPointerException().isThrownBy(() -> adapter.issue(null));
        assertThatNullPointerException().isThrownBy(() -> adapter.matches(
                null, RefreshTokenHash.encoded("H".repeat(60))));
        assertThatNullPointerException().isThrownBy(() -> adapter.matches(
                RefreshTokenCredential.create(RefreshTokenId.newId(), "S".repeat(43)), null));
    }

    @Test
    void delegatesTokenTimeToInjectedClock() {
        Instant now = Instant.parse("2026-07-05T06:00:00Z");
        var adapter = new SystemTokenClockAdapter(Clock.fixed(now, ZoneOffset.UTC));

        assertThat(adapter.now()).isEqualTo(now);
    }
}
