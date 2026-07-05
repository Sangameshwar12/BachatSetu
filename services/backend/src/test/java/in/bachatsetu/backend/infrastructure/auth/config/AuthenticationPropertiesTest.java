package in.bachatsetu.backend.infrastructure.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuthenticationPropertiesTest {

    @Test
    void rejectsConfigurationThatChangesDomainPolicyOrWeakensBcrypt() {
        assertInvalid("otp-validity=4m", "OTP validity must remain five minutes");
        assertInvalid("resend-limit=2", "OTP resend limit must remain three");
        assertInvalid("verify-limit=4", "OTP verification limit must remain five");
        assertInvalid("hash-strength=9", "BCrypt hash strength must be between 10 and 16");
        assertInvalid("hash-strength=17", "BCrypt hash strength must be between 10 and 16");
    }

    private void assertInvalid(String override, String expectedMessage) {
        new ApplicationContextRunner()
                .withUserConfiguration(AuthenticationInfrastructureConfig.class)
                .withPropertyValues(
                        "bachatsetu.authentication.otp-validity=5m",
                        "bachatsetu.authentication.resend-limit=3",
                        "bachatsetu.authentication.verify-limit=5",
                        "bachatsetu.authentication.hash-strength=10",
                        "bachatsetu.authentication." + override)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(expectedMessage);
                });
    }
}
