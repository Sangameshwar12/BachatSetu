package in.bachatsetu.backend.infrastructure.email;

import in.bachatsetu.backend.email.application.event.EmailSendFailed;
import in.bachatsetu.backend.email.application.event.EmailSent;
import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.email.domain.model.EmailAddress;
import in.bachatsetu.backend.email.domain.model.EmailContent;
import in.bachatsetu.backend.email.domain.model.EmailDeliveryStatus;
import in.bachatsetu.backend.email.domain.model.EmailMessage;
import in.bachatsetu.backend.email.domain.model.EmailSendResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Delivers email through whichever {@link EmailProviderClient} is configured, orchestrating
 * retry, metrics, health tracking, audit-facing event publication, and masked logging — none of
 * which any provider client or any business module needs to know about. Retries only transient
 * failures ({@link EmailProviderException#retryable()}); immediately gives up on a
 * provider-rejected request. Mirrors {@code SmsOtpSenderAdapter} exactly, including retrying
 * without a backoff delay (this codebase's architecture rules forbid {@code Thread.sleep} outside
 * a scheduling mechanism, and {@code retryCount} is small by design).
 *
 * <p>Never throws: a caller always gets back an {@link EmailSendResult} describing whether
 * delivery ultimately succeeded, so a business flow that triggers an email (signup, an
 * invitation) never fails because of an email provider outage.
 */
public final class RetryingEmailSenderAdapter implements EmailSenderPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryingEmailSenderAdapter.class);
    private static final String METRIC_PROVIDER_TAG = "provider";
    private static final int VISIBLE_LOCAL_PART_CHARS = 2;

    private final EmailProviderClient client;
    private final EmailProviderType providerType;
    private final int retryCount;
    private final String fromAddress;
    private final String replyTo;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailProviderHealthTracker healthTracker;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public RetryingEmailSenderAdapter(
            EmailProviderClient client,
            EmailProviderType providerType,
            int retryCount,
            String fromAddress,
            String replyTo,
            ApplicationEventPublisher eventPublisher,
            EmailProviderHealthTracker healthTracker,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.client = Objects.requireNonNull(client, "email provider client must not be null");
        this.providerType = Objects.requireNonNull(providerType, "provider type must not be null");
        if (retryCount < 0) {
            throw new IllegalArgumentException("retry count must not be negative");
        }
        this.retryCount = retryCount;
        this.fromAddress = Objects.requireNonNull(fromAddress, "fromAddress must not be null");
        this.replyTo = Objects.requireNonNull(replyTo, "replyTo must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
        this.healthTracker = Objects.requireNonNull(healthTracker, "health tracker must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meter registry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public EmailSendResult send(EmailMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        String maskedRecipient = mask(message.to());
        EmailProviderMessage providerMessage = toProviderMessage(message);
        Timer.Sample sample = Timer.start(meterRegistry);
        Attempt outcome = attemptWithRetries(providerMessage, maskedRecipient);
        long elapsedNanos = sample.stop(meterRegistry.timer("email.duration", METRIC_PROVIDER_TAG, providerType.name()));
        long durationMillis = Duration.ofNanos(elapsedNanos).toMillis();
        Instant now = clock.instant();
        if (outcome.success() != null) {
            meterRegistry.counter("email.sent.success", METRIC_PROVIDER_TAG, providerType.name()).increment();
            healthTracker.recordSuccess();
            eventPublisher.publishEvent(new EmailSent(
                    UUID.randomUUID(), maskedRecipient, message.category(), providerType.name(),
                    outcome.success().providerMessageId(), durationMillis, now));
            return new EmailSendResult(
                    EmailDeliveryStatus.SENT, providerType.name(), outcome.success().providerMessageId(), now, null);
        }
        meterRegistry.counter("email.sent.failure", METRIC_PROVIDER_TAG, providerType.name()).increment();
        healthTracker.recordFailure();
        LOGGER.warn(
                "Email delivery failed after exhausting retries. provider={} recipient={} category={} timestamp={}",
                providerType, maskedRecipient, message.category(), now);
        eventPublisher.publishEvent(new EmailSendFailed(
                UUID.randomUUID(), maskedRecipient, message.category(), providerType.name(),
                outcome.failure().getMessage(), durationMillis, now));
        return new EmailSendResult(
                EmailDeliveryStatus.FAILED, providerType.name(), null, now, outcome.failure().getMessage());
    }

    /**
     * Returns the successful result, or the last failure once retries are exhausted — never both.
     * Written as {@code while (true)} deliberately: every branch already returns, so the compiler
     * can prove this method never falls through without a redundant trailing statement.
     */
    private Attempt attemptWithRetries(EmailProviderMessage message, String maskedRecipient) {
        int attempt = 0;
        while (true) {
            try {
                EmailProviderSendResult result = client.send(message);
                LOGGER.info(
                        "Email delivered. provider={} recipient={} providerMessageId={} attempt={}",
                        providerType, maskedRecipient, result.providerMessageId(), attempt);
                return Attempt.success(result);
            } catch (EmailProviderException exception) {
                boolean hasRetriesLeft = attempt < retryCount;
                if (!exception.retryable() || !hasRetriesLeft) {
                    return Attempt.failure(exception);
                }
                meterRegistry.counter("email.retry", METRIC_PROVIDER_TAG, providerType.name()).increment();
                LOGGER.warn(
                        "Email delivery attempt {} failed and will be retried. provider={} recipient={}",
                        attempt, providerType, maskedRecipient);
                attempt++;
            }
        }
    }

    private EmailProviderMessage toProviderMessage(EmailMessage message) {
        EmailContent content = message.content();
        return new EmailProviderMessage(
                message.to().value(), fromAddress, replyTo, content.subject(), content.htmlBody(), content.textBody());
    }

    private String mask(EmailAddress address) {
        String value = address.value();
        int at = value.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String localPart = value.substring(0, at);
        String domain = value.substring(at);
        if (localPart.length() <= VISIBLE_LOCAL_PART_CHARS) {
            return "*".repeat(localPart.length()) + domain;
        }
        return localPart.substring(0, VISIBLE_LOCAL_PART_CHARS)
                + "*".repeat(localPart.length() - VISIBLE_LOCAL_PART_CHARS)
                + domain;
    }

    /** Exactly one of {@link #success} or {@link #failure} is non-null. */
    private record Attempt(EmailProviderSendResult success, EmailProviderException failure) {
        static Attempt success(EmailProviderSendResult result) {
            return new Attempt(result, null);
        }

        static Attempt failure(EmailProviderException exception) {
            return new Attempt(null, exception);
        }
    }
}
