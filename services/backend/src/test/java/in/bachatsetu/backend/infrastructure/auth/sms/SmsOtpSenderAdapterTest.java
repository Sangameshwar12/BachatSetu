package in.bachatsetu.backend.infrastructure.auth.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import in.bachatsetu.backend.auth.application.event.OtpSendFailed;
import in.bachatsetu.backend.auth.application.exception.OtpApplicationException;
import in.bachatsetu.backend.auth.application.exception.OtpFailureReason;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * No live network call happens anywhere in this test — {@link FakeSmsProviderClient} is an
 * in-memory stand-in for a real {@link SmsProviderClient}, exercising only
 * {@link SmsOtpSenderAdapter}'s own orchestration: retry, metrics, health tracking, audit-facing
 * event publication, and log masking.
 */
class SmsOtpSenderAdapterTest {

    private static final UserId USER_ID = UserId.newId();
    private static final MobileNumber MOBILE_NUMBER = MobileNumber.of("+919876543210");
    private static final OtpCode OTP_CODE = OtpCode.of("123456");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-11T10:00:00Z"), ZoneOffset.UTC);

    private final List<Object> publishedEvents = new ArrayList<>();
    private final ApplicationEventPublisher eventPublisher = publishedEvents::add;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final SmsProviderHealthTracker healthTracker = new SmsProviderHealthTracker();
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void captureLogs() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(SmsOtpSenderAdapter.class)).addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(SmsOtpSenderAdapter.class)).detachAppender(logAppender);
    }

    @Test
    void refusesANegativeRetryCount() {
        assertThatThrownBy(() -> new SmsOtpSenderAdapter(
                        FakeSmsProviderClient.alwaysSucceeds(), SmsProviderType.MSG91, -1,
                        eventPublisher, healthTracker, meterRegistry, CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void succeedsOnTheFirstAttemptWithoutRetrying() {
        FakeSmsProviderClient client = FakeSmsProviderClient.alwaysSucceeds();
        SmsOtpSenderAdapter adapter = adapter(client, 2);

        adapter.send(USER_ID, MOBILE_NUMBER, OtpPurpose.SIGN_IN, OTP_CODE);

        assertThat(client.attempts).isEqualTo(1);
        assertThat(meterRegistry.counter("sms.sent.success", "provider", "MSG91").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("sms.sent.failure", "provider", "MSG91").count()).isZero();
        assertThat(meterRegistry.counter("sms.retry", "provider", "MSG91").count()).isZero();
        assertThat(meterRegistry.timer("sms.duration", "provider", "MSG91").count()).isEqualTo(1);
        assertThat(healthTracker.status()).isEqualTo(SmsProviderHealthStatus.UP);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void retriesATransientFailureAndSucceeds() {
        FakeSmsProviderClient client = FakeSmsProviderClient.failsThenSucceeds(
                new SmsProviderException("timeout", true, -1));
        SmsOtpSenderAdapter adapter = adapter(client, 2);

        adapter.send(USER_ID, MOBILE_NUMBER, OtpPurpose.SIGN_IN, OTP_CODE);

        assertThat(client.attempts).isEqualTo(2);
        assertThat(meterRegistry.counter("sms.sent.success", "provider", "MSG91").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("sms.retry", "provider", "MSG91").count()).isEqualTo(1.0);
        assertThat(healthTracker.status()).isEqualTo(SmsProviderHealthStatus.UP);
    }

    @Test
    void doesNotRetryANonRetryableFailureEvenWithRetriesRemaining() {
        FakeSmsProviderClient client = FakeSmsProviderClient.alwaysFails(
                new SmsProviderException("invalid request", false, 400));
        SmsOtpSenderAdapter adapter = adapter(client, 3);

        assertThatThrownBy(() -> adapter.send(USER_ID, MOBILE_NUMBER, OtpPurpose.SIGN_IN, OTP_CODE))
                .isInstanceOf(OtpApplicationException.class);

        assertThat(client.attempts).isEqualTo(1);
        assertThat(meterRegistry.counter("sms.retry", "provider", "MSG91").count()).isZero();
    }

    @Test
    void translatesExhaustedRetriesIntoTheExistingOtpApplicationExceptionWithoutLeakingProviderDetails() {
        FakeSmsProviderClient client = FakeSmsProviderClient.alwaysFails(
                new SmsProviderException("MSG91 request failed with HTTP 503: raw-provider-body-detail", true, 503));
        SmsOtpSenderAdapter adapter = adapter(client, 1);

        assertThatThrownBy(() -> adapter.send(USER_ID, MOBILE_NUMBER, OtpPurpose.SIGN_IN, OTP_CODE))
                .isInstanceOfSatisfying(OtpApplicationException.class, exception -> {
                    assertThat(exception.reason()).isEqualTo(OtpFailureReason.SMS_DELIVERY_FAILED);
                    assertThat(exception.getMessage()).doesNotContain("raw-provider-body-detail");
                });

        assertThat(client.attempts).isEqualTo(2);
        assertThat(meterRegistry.counter("sms.sent.failure", "provider", "MSG91").count()).isEqualTo(1.0);
        assertThat(healthTracker.consecutiveFailures()).isEqualTo(1);
        assertThat(publishedEvents).singleElement().isInstanceOfSatisfying(OtpSendFailed.class, event -> {
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.purpose()).isEqualTo(OtpPurpose.SIGN_IN);
            assertThat(event.provider()).isEqualTo("MSG91");
        });
    }

    @Test
    void reportsProviderDownAfterThreeConsecutiveFailureBatches() {
        FakeSmsProviderClient client = FakeSmsProviderClient.alwaysFails(
                new SmsProviderException("rejected", false, 400));
        SmsOtpSenderAdapter adapter = adapter(client, 0);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> adapter.send(USER_ID, MOBILE_NUMBER, OtpPurpose.SIGN_IN, OTP_CODE))
                    .isInstanceOf(OtpApplicationException.class);
        }

        assertThat(healthTracker.status()).isEqualTo(SmsProviderHealthStatus.DOWN);
    }

    @Test
    void neverLogsTheOtpOrTheUnmaskedPhoneNumber() {
        FakeSmsProviderClient client = FakeSmsProviderClient.alwaysSucceeds();
        SmsOtpSenderAdapter adapter = adapter(client, 0);

        adapter.send(USER_ID, MOBILE_NUMBER, OtpPurpose.SIGN_IN, OTP_CODE);

        List<String> messages = logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).isNotEmpty();
        assertThat(messages).noneMatch(message -> message.contains(OTP_CODE.value()));
        assertThat(messages).noneMatch(message -> message.contains(MOBILE_NUMBER.value()));
        assertThat(messages).anyMatch(message -> message.contains("+91******3210"));
    }

    @Test
    void neverLogsTheOtpOrTheUnmaskedPhoneNumberOnFailure() {
        FakeSmsProviderClient client = FakeSmsProviderClient.alwaysFails(
                new SmsProviderException("rejected", false, 400));
        SmsOtpSenderAdapter adapter = adapter(client, 0);

        assertThatThrownBy(() -> adapter.send(USER_ID, MOBILE_NUMBER, OtpPurpose.SIGN_IN, OTP_CODE))
                .isInstanceOf(OtpApplicationException.class);

        List<String> messages = logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).noneMatch(message -> message.contains(OTP_CODE.value()));
        assertThat(messages).noneMatch(message -> message.contains(MOBILE_NUMBER.value()));
        assertThat(messages).anyMatch(message -> message.contains("+91******3210"));
    }

    private SmsOtpSenderAdapter adapter(SmsProviderClient client, int retryCount) {
        return new SmsOtpSenderAdapter(
                client, SmsProviderType.MSG91, retryCount, eventPublisher, healthTracker, meterRegistry, CLOCK);
    }

    /**
     * A queue of outcomes to hand back, one per call to {@link #send(SmsMessage)}: an
     * {@link SmsSendResult} is returned, an {@link SmsProviderException} is thrown. The last
     * queued outcome repeats once the queue is drained, so "always succeeds"/"always fails" only
     * need a single queued entry.
     */
    private static final class FakeSmsProviderClient implements SmsProviderClient {
        private final Queue<Object> outcomes;
        private int attempts;

        private FakeSmsProviderClient(Queue<Object> outcomes) {
            this.outcomes = outcomes;
        }

        static FakeSmsProviderClient alwaysSucceeds() {
            Queue<Object> outcomes = new ArrayDeque<>();
            outcomes.add(new SmsSendResult("MSG91", "req-1", Instant.now()));
            return new FakeSmsProviderClient(outcomes);
        }

        static FakeSmsProviderClient failsThenSucceeds(SmsProviderException firstFailure) {
            Queue<Object> outcomes = new ArrayDeque<>();
            outcomes.add(firstFailure);
            outcomes.add(new SmsSendResult("MSG91", "req-2", Instant.now()));
            return new FakeSmsProviderClient(outcomes);
        }

        static FakeSmsProviderClient alwaysFails(SmsProviderException failure) {
            Queue<Object> outcomes = new ArrayDeque<>();
            outcomes.add(failure);
            return new FakeSmsProviderClient(outcomes);
        }

        @Override
        public SmsSendResult send(SmsMessage message) {
            attempts++;
            Object outcome = outcomes.size() > 1 ? outcomes.poll() : outcomes.peek();
            if (outcome instanceof SmsProviderException exception) {
                throw exception;
            }
            return (SmsSendResult) outcome;
        }
    }
}
