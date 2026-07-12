package in.bachatsetu.backend.infrastructure.auth.config;

import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.infrastructure.auth.sms.Fast2SmsSmsProviderClient;
import in.bachatsetu.backend.infrastructure.auth.sms.Msg91SmsProviderClient;
import in.bachatsetu.backend.infrastructure.auth.sms.SmsOtpSenderAdapter;
import in.bachatsetu.backend.infrastructure.auth.sms.SmsProviderClient;
import in.bachatsetu.backend.infrastructure.auth.sms.SmsProviderHealthIndicator;
import in.bachatsetu.backend.infrastructure.auth.sms.SmsProviderHealthTracker;
import in.bachatsetu.backend.infrastructure.auth.sms.TwilioSmsProviderClient;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wires the real SMS provider integration — active for every profile except {@code local} and
 * {@code test} (see {@link LocalOtpSenderConfig}), which keep the log-only sender so neither
 * interactive development nor the test suite ever requires live SMS credentials.
 *
 * <p>Exactly one of the three {@code SmsProviderClient} beans below is created, selected by
 * {@code bachatsetu.sms.provider} ({@code SMS_PROVIDER}) — switching providers is purely this
 * one configuration change; {@link SmsOtpSenderAdapter} and every OTP application service are
 * unaware which provider is active. {@link SmsProviderProperties}'s compact constructor already
 * fails application startup if the selected provider's secrets are missing, so this class adds
 * no further validation — it only assembles already-validated configuration into beans.
 */
@Configuration(proxyBeanMethods = false)
@Profile({"dev"})
@EnableConfigurationProperties(SmsProviderProperties.class)
public class SmsInfrastructureConfig {

    @Bean
    RestClient smsRestClient(SmsProviderProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.sms", name = "provider", havingValue = "MSG91")
    SmsProviderClient msg91SmsProviderClient(RestClient smsRestClient, SmsProviderProperties properties) {
        return new Msg91SmsProviderClient(smsRestClient, properties.msg91());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.sms", name = "provider", havingValue = "FAST2SMS")
    SmsProviderClient fast2SmsSmsProviderClient(RestClient smsRestClient, SmsProviderProperties properties) {
        return new Fast2SmsSmsProviderClient(smsRestClient, properties.fast2sms());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.sms", name = "provider", havingValue = "TWILIO")
    SmsProviderClient twilioSmsProviderClient(RestClient smsRestClient, SmsProviderProperties properties) {
        return new TwilioSmsProviderClient(smsRestClient, properties.twilio());
    }

    @Bean
    SmsProviderHealthTracker smsProviderHealthTracker() {
        return new SmsProviderHealthTracker();
    }

    /** Bean name drives the actuator component name: {@code /actuator/health/smsProvider}. */
    @Bean
    HealthIndicator smsProviderHealthIndicator(SmsProviderProperties properties, SmsProviderHealthTracker tracker) {
        return new SmsProviderHealthIndicator(properties.provider(), tracker);
    }

    @Bean
    OtpSenderPort smsOtpSenderAdapter(
            SmsProviderClient smsProviderClient,
            SmsProviderProperties properties,
            ApplicationEventPublisher eventPublisher,
            SmsProviderHealthTracker healthTracker,
            MeterRegistry meterRegistry,
            Clock authenticationClock) {
        return new SmsOtpSenderAdapter(
                smsProviderClient,
                properties.provider(),
                properties.retryCount(),
                eventPublisher,
                healthTracker,
                meterRegistry,
                authenticationClock);
    }
}
