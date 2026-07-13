package in.bachatsetu.backend.infrastructure.auth.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.domain.model.OtpCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

// TEMPORARY MVP TEST OTP — REMOVE BEFORE PRODUCTION
@ExtendWith(OutputCaptureExtension.class)
class FixedTestOtpGeneratorAdapterTest {

    @Test
    void alwaysGeneratesTheFixedTestCode() {
        FixedTestOtpGeneratorAdapter adapter = new FixedTestOtpGeneratorAdapter();

        for (int sample = 0; sample < 5; sample++) {
            assertThat(adapter.generateOtp()).isEqualTo(OtpCode.of("102030"));
        }
    }

    @Test
    void logsAWarningBannerOnConstruction(CapturedOutput output) {
        new FixedTestOtpGeneratorAdapter();

        assertThat(output)
                .contains("TEST OTP MODE ENABLED")
                .contains("ALL USERS WILL USE OTP: 102030")
                .contains("REMOVE BEFORE PRODUCTION");
    }
}
