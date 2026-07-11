package in.bachatsetu.backend.infrastructure.auth.sms;

import in.bachatsetu.backend.auth.application.event.OtpSendFailed;
import in.bachatsetu.backend.auth.application.exception.OtpApplicationException;
import in.bachatsetu.backend.auth.application.exception.OtpFailureReason;
import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Delivers OTPs through whichever {@link SmsProviderClient} is configured, orchestrating retry,
 * metrics, health tracking, audit-facing event publication, and masked logging — none of which
 * any provider client or any OTP application service needs to know about. Retries only
 * transient failures ({@link SmsProviderException#retryable()}); immediately gives up on a
 * provider-rejected request. Retries are attempted without a backoff delay: this codebase's own
 * architecture rules forbid {@code Thread.sleep} outside a scheduling mechanism (see
 * {@code ForbiddenApiArchitectureTest}), and {@code retryCount} is small (a handful of attempts
 * at most) by design, so an immediate retry is an intentional, simple choice here rather than an
 * oversight.
 */
public final class SmsOtpSenderAdapter implements OtpSenderPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsOtpSenderAdapter.class);
    private static final int VISIBLE_DIGITS = 4;
    private static final int COUNTRY_PREFIX_LENGTH = 3;
    private static final String METRIC_PROVIDER_TAG = "provider";

    private final SmsProviderClient client;
    private final SmsProviderType providerType;
    private final int retryCount;
    private final ApplicationEventPublisher eventPublisher;
    private final SmsProviderHealthTracker healthTracker;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public SmsOtpSenderAdapter(
            SmsProviderClient client,
            SmsProviderType providerType,
            int retryCount,
            ApplicationEventPublisher eventPublisher,
            SmsProviderHealthTracker healthTracker,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.client = Objects.requireNonNull(client, "SMS provider client must not be null");
        this.providerType = Objects.requireNonNull(providerType, "provider type must not be null");
        if (retryCount < 0) {
            throw new IllegalArgumentException("retry count must not be negative");
        }
        this.retryCount = retryCount;
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
        this.healthTracker = Objects.requireNonNull(healthTracker, "health tracker must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meter registry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void send(UserId userId, MobileNumber mobileNumber, OtpPurpose purpose, OtpCode code) {
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(mobileNumber, "mobile number must not be null");
        Objects.requireNonNull(purpose, "OTP purpose must not be null");
        Objects.requireNonNull(code, "OTP code must not be null");
        String maskedNumber = mask(mobileNumber);
        SmsMessage message = new SmsMessage(
                mobileNumber.value(),
                code.value(),
                "Your BachatSetu OTP is " + code.value() + ". It is valid for 5 minutes. "
                        + "Do not share this code with anyone.");
        Timer.Sample sample = Timer.start(meterRegistry);
        SmsProviderException lastFailure = attemptWithRetries(message, maskedNumber);
        sample.stop(meterRegistry.timer("sms.duration", METRIC_PROVIDER_TAG, providerType.name()));
        if (lastFailure == null) {
            return;
        }
        meterRegistry.counter("sms.sent.failure", METRIC_PROVIDER_TAG, providerType.name()).increment();
        healthTracker.recordFailure();
        LOGGER.warn(
                "OTP SMS delivery failed after exhausting retries. provider={} destination={} timestamp={}",
                providerType,
                maskedNumber,
                clock.instant());
        eventPublisher.publishEvent(new OtpSendFailed(
                UUID.randomUUID(), userId, purpose, providerType.name(), lastFailure.getMessage(), clock.instant()));
        throw new OtpApplicationException(
                OtpFailureReason.SMS_DELIVERY_FAILED,
                "Unable to deliver the OTP by SMS at this time. Please try again shortly.");
    }

    /**
     * Returns {@code null} on success, or the last {@link SmsProviderException} once retries are
     * exhausted. Written as {@code while (true)} rather than a bounded {@code for} loop
     * deliberately: every branch inside the loop body already {@code return}s, so the compiler
     * can prove this method never falls through without a redundant, untestable trailing
     * statement after the loop.
     */
    private SmsProviderException attemptWithRetries(SmsMessage message, String maskedNumber) {
        int attempt = 0;
        while (true) {
            try {
                SmsSendResult result = client.send(message);
                meterRegistry.counter("sms.sent.success", METRIC_PROVIDER_TAG, providerType.name()).increment();
                healthTracker.recordSuccess();
                LOGGER.info(
                        "OTP SMS delivered. provider={} destination={} providerMessageId={} attempt={}",
                        providerType,
                        maskedNumber,
                        result.providerMessageId(),
                        attempt);
                return null;
            } catch (SmsProviderException exception) {
                boolean hasRetriesLeft = attempt < retryCount;
                if (!exception.retryable() || !hasRetriesLeft) {
                    return exception;
                }
                meterRegistry.counter("sms.retry", METRIC_PROVIDER_TAG, providerType.name()).increment();
                LOGGER.warn(
                        "OTP SMS delivery attempt {} failed and will be retried. provider={} destination={}",
                        attempt,
                        providerType,
                        maskedNumber);
                attempt++;
            }
        }
    }

    private String mask(MobileNumber mobileNumber) {
        String value = mobileNumber.value();
        String nationalNumber = value.substring(COUNTRY_PREFIX_LENGTH);
        return value.substring(0, COUNTRY_PREFIX_LENGTH)
                + "*".repeat(nationalNumber.length() - VISIBLE_DIGITS)
                + nationalNumber.substring(nationalNumber.length() - VISIBLE_DIGITS);
    }
}
