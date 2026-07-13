package in.bachatsetu.backend.infrastructure.email.config;

import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.infrastructure.email.AwsSesEmailProviderClient;
import in.bachatsetu.backend.infrastructure.email.EmailProviderClient;
import in.bachatsetu.backend.infrastructure.email.EmailProviderHealthIndicator;
import in.bachatsetu.backend.infrastructure.email.EmailProviderHealthTracker;
import in.bachatsetu.backend.infrastructure.email.ResendEmailProviderClient;
import in.bachatsetu.backend.infrastructure.email.RetryingEmailSenderAdapter;
import in.bachatsetu.backend.infrastructure.email.SendGridEmailProviderClient;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * Wires the real email provider integration — active only when a real provider is explicitly
 * enabled ({@code bachatsetu.email.enabled}, env {@code EMAIL_PROVIDER_ENABLED}, default
 * {@code false}). This is a deployment-mode switch, not an environment one: whether real email
 * is wired is orthogonal to which Spring profile (local/dev/test/prod) is active — an MVP closed
 * beta legitimately runs the {@code prod} profile (strict CORS, no Swagger, production database)
 * while still wanting log-only delivery. See {@link LocalEmailSenderConfig}, which is active
 * under the exact opposite condition, so precisely one {@link EmailSenderPort} bean ever exists
 * regardless of which Spring profile is running.
 *
 * <p>Exactly one of the three {@code EmailProviderClient} beans below is created, selected by
 * {@code bachatsetu.email.provider} ({@code EMAIL_PROVIDER}) — switching providers is purely this
 * one configuration change; {@link RetryingEmailSenderAdapter} and every business module are
 * unaware which provider is active. {@link EmailProviderProperties}'s compact constructor already
 * fails application startup if the selected provider's secrets (or {@code EMAIL_FROM_ADDRESS})
 * are missing, so this class adds no further validation — and because that properties class is
 * only bound while this configuration class is itself active, migrating to a real provider is
 * purely a configuration change: set {@code EMAIL_PROVIDER_ENABLED=true} plus that provider's
 * credentials, no code change.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "bachatsetu.email", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(EmailProviderProperties.class)
public class EmailInfrastructureConfig {

    @Bean
    RestClient emailRestClient(EmailProviderProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.email", name = "provider", havingValue = "AWS_SES")
    SesClient sesClient(EmailProviderProperties properties, Clock authenticationClock) {
        EmailProviderProperties.AwsSes config = properties.awsSes();
        return SesClient.builder()
                .region(Region.of(config.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKey(), config.secretKey())))
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(properties.connectTimeout())
                        .socketTimeout(properties.readTimeout()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.email", name = "provider", havingValue = "AWS_SES")
    EmailProviderClient awsSesEmailProviderClient(SesClient sesClient) {
        return new AwsSesEmailProviderClient(sesClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.email", name = "provider", havingValue = "RESEND")
    EmailProviderClient resendEmailProviderClient(RestClient emailRestClient, EmailProviderProperties properties) {
        return new ResendEmailProviderClient(emailRestClient, properties.resend());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.email", name = "provider", havingValue = "SENDGRID")
    EmailProviderClient sendGridEmailProviderClient(RestClient emailRestClient, EmailProviderProperties properties) {
        return new SendGridEmailProviderClient(emailRestClient, properties.sendGrid());
    }

    @Bean
    EmailProviderHealthTracker emailProviderHealthTracker() {
        return new EmailProviderHealthTracker();
    }

    /** Bean name drives the actuator component name: {@code /actuator/health/emailProvider}. */
    @Bean
    HealthIndicator emailProviderHealthIndicator(
            EmailProviderProperties properties, EmailProviderHealthTracker tracker) {
        return new EmailProviderHealthIndicator(properties.provider(), tracker);
    }

    @Bean
    EmailSenderPort retryingEmailSenderAdapter(
            EmailProviderClient emailProviderClient,
            EmailProviderProperties properties,
            ApplicationEventPublisher eventPublisher,
            EmailProviderHealthTracker healthTracker,
            MeterRegistry meterRegistry,
            Clock authenticationClock) {
        return new RetryingEmailSenderAdapter(
                emailProviderClient,
                properties.provider(),
                properties.retryCount(),
                properties.fromAddress(),
                properties.replyTo(),
                eventPublisher,
                healthTracker,
                meterRegistry,
                authenticationClock);
    }
}
