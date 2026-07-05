package in.bachatsetu.backend.infrastructure.auth.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(OutputCaptureExtension.class)
class AuthenticationAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");

    @Test
    void systemClockDelegatesToInjectedJavaClock() {
        SystemClockAdapter adapter = new SystemClockAdapter(Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(adapter.now()).isEqualTo(NOW);
    }

    @Test
    void secureRandomGeneratesVariableSixDigitValues() {
        SecureRandomGeneratorAdapter adapter = new SecureRandomGeneratorAdapter(new SecureRandom());
        Set<String> generatedValues = new HashSet<>();

        for (int sample = 0; sample < 200; sample++) {
            String value = adapter.generateOtp().value();
            assertThat(value).matches("[0-9]{6}");
            assertThat(Integer.parseInt(value)).isBetween(100_000, 999_999);
            generatedValues.add(value);
        }

        assertThat(generatedValues).hasSizeGreaterThan(1);
    }

    @Test
    void bcryptHashesWithSaltAndVerifiesCandidates() {
        BCryptHashingAdapter adapter = new BCryptHashingAdapter(new BCryptPasswordEncoder(4));
        OtpCode code = OtpCode.of("123456");

        OtpHash firstHash = adapter.hash(code);
        OtpHash secondHash = adapter.hash(code);

        assertThat(firstHash.value()).startsWith("$2a$04$").isNotEqualTo(secondHash.value());
        assertThat(firstHash.value()).doesNotContain(code.value());
        assertThat(adapter.matches(code, firstHash)).isTrue();
        assertThat(adapter.matches(OtpCode.of("654321"), firstHash)).isFalse();
    }

    @Test
    void localSenderLogsOnlyMaskedMetadata(CapturedOutput output) {
        UUID correlationId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        LoggingOtpSenderAdapter adapter = new LoggingOtpSenderAdapter(
                Clock.fixed(NOW, ZoneOffset.UTC), () -> correlationId);
        OtpCode code = OtpCode.of("123456");
        MobileNumber destination = MobileNumber.of("+919876543210");

        adapter.send(UserId.newId(), destination, OtpPurpose.SIGN_IN, code);

        assertThat(output)
                .contains("OTP generated successfully")
                .contains("destination=+91******3210")
                .contains("correlationId=" + correlationId)
                .contains("timestamp=" + NOW)
                .doesNotContain(code.value())
                .doesNotContain(destination.value());
    }
}
