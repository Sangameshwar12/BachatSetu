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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wires the real SMS provider integration — active only when a real provider is explicitly
 * enabled ({@code bachatsetu.sms.enabled}, env {@code SMS_PROVIDER_ENABLED}, default
 * {@code false}). This is a deployment-mode switch, not an environment one: whether real SMS is
 * wired is orthogonal to which Spring profile (local/dev/test/prod) is active — an MVP closed
 * beta legitimately runs the {@code prod} profile (strict CORS, no Swagger, production database)
 * while still wanting log-only OTP delivery. See {@link LocalOtpSenderConfig}, which is active
 * under the exact opposite condition, so precisely one {@link OtpSenderPort} bean ever exists
 * regardless of which Spring profile is running.
 *
 * <p>Exactly one of the three {@code SmsProviderClient} beans below is created, selected by
 * {@code bachatsetu.sms.provider} ({@code SMS_PROVIDER}) — switching providers is purely this
 * one configuration change; {@link SmsOtpSenderAdapter} and every OTP application service are
 * unaware which provider is active. {@link SmsProviderProperties}'s compact constructor already
 * fails application startup if the selected provider's secrets are missing, so this class adds
 * no further validation — and because that properties class is only bound while this
 * configuration class is itself active, migrating to a real provider is purely a configuration
 * change: set {@code SMS_PROVIDER_ENABLED=true} plus that provider's credentials, no code change.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "bachatsetu.sms", name = "enabled", havingValue = "true")
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
