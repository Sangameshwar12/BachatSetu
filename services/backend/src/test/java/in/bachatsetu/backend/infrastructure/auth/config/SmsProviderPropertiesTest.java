package in.bachatsetu.backend.infrastructure.auth.config;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.infrastructure.auth.sms.SmsProviderType;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SmsProviderPropertiesTest {

    private static final SmsProviderProperties.Msg91 CONFIGURED_MSG91 =
            new SmsProviderProperties.Msg91("auth-key", "template-id", "SENDER");
    private static final SmsProviderProperties.Msg91 BLANK_MSG91 =
            new SmsProviderProperties.Msg91("", "", "");
    private static final SmsProviderProperties.Fast2Sms CONFIGURED_FAST2SMS =
            new SmsProviderProperties.Fast2Sms("api-key");
    private static final SmsProviderProperties.Fast2Sms BLANK_FAST2SMS = new SmsProviderProperties.Fast2Sms("");
    private static final SmsProviderProperties.Twilio CONFIGURED_TWILIO =
            new SmsProviderProperties.Twilio("sid", "token", "+15005550006");
    private static final SmsProviderProperties.Twilio BLANK_TWILIO =
            new SmsProviderProperties.Twilio("", "", "");

    @Test
    void startsWhenTheSelectedProviderIsFullyConfigured() {
        assertThatNoException().isThrownBy(() -> new SmsProviderProperties(
                SmsProviderType.MSG91, 2, Duration.ofSeconds(3), Duration.ofSeconds(5),
                CONFIGURED_MSG91, BLANK_FAST2SMS, BLANK_TWILIO));
        assertThatNoException().isThrownBy(() -> new SmsProviderProperties(
                SmsProviderType.FAST2SMS, 2, Duration.ofSeconds(3), Duration.ofSeconds(5),
                BLANK_MSG91, CONFIGURED_FAST2SMS, BLANK_TWILIO));
        assertThatNoException().isThrownBy(() -> new SmsProviderProperties(
                SmsProviderType.TWILIO, 2, Duration.ofSeconds(3), Duration.ofSeconds(5),
                BLANK_MSG91, BLANK_FAST2SMS, CONFIGURED_TWILIO));
    }

    @Test
    void refusesToStartWhenTheSelectedProviderIsMissingItsSecrets() {
        assertThatThrownBy(() -> new SmsProviderProperties(
                SmsProviderType.MSG91, 2, Duration.ofSeconds(3), Duration.ofSeconds(5),
                BLANK_MSG91, CONFIGURED_FAST2SMS, CONFIGURED_TWILIO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG91_AUTH_KEY");

        assertThatThrownBy(() -> new SmsProviderProperties(
                SmsProviderType.FAST2SMS, 2, Duration.ofSeconds(3), Duration.ofSeconds(5),
                CONFIGURED_MSG91, BLANK_FAST2SMS, CONFIGURED_TWILIO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FAST2SMS_API_KEY");

        assertThatThrownBy(() -> new SmsProviderProperties(
                SmsProviderType.TWILIO, 2, Duration.ofSeconds(3), Duration.ofSeconds(5),
                CONFIGURED_MSG91, CONFIGURED_FAST2SMS, BLANK_TWILIO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TWILIO_ACCOUNT_SID");
    }

    @Test
    void doesNotRequireAnUnselectedProvidersSecretsToBeConfigured() {
        assertThatNoException().isThrownBy(() -> new SmsProviderProperties(
                SmsProviderType.MSG91, 2, Duration.ofSeconds(3), Duration.ofSeconds(5),
                CONFIGURED_MSG91, BLANK_FAST2SMS, BLANK_TWILIO));
    }

    @Test
    void refusesANegativeOrExcessiveRetryCount() {
        assertThatThrownBy(() -> new SmsProviderProperties(
                SmsProviderType.MSG91, -1, Duration.ofSeconds(3), Duration.ofSeconds(5),
                CONFIGURED_MSG91, BLANK_FAST2SMS, BLANK_TWILIO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SmsProviderProperties(
                SmsProviderType.MSG91, 6, Duration.ofSeconds(3), Duration.ofSeconds(5),
                CONFIGURED_MSG91, BLANK_FAST2SMS, BLANK_TWILIO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
