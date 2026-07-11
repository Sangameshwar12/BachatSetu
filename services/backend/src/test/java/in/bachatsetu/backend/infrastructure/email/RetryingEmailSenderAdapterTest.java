package in.bachatsetu.backend.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import in.bachatsetu.backend.email.application.event.EmailSendFailed;
import in.bachatsetu.backend.email.application.event.EmailSent;
import in.bachatsetu.backend.email.domain.model.EmailAddress;
import in.bachatsetu.backend.email.domain.model.EmailContent;
import in.bachatsetu.backend.email.domain.model.EmailDeliveryStatus;
import in.bachatsetu.backend.email.domain.model.EmailMessage;
import in.bachatsetu.backend.email.domain.model.EmailSendResult;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
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
 * No live network call happens anywhere in this test — {@link FakeEmailProviderClient} is an
 * in-memory stand-in for a real {@link EmailProviderClient}, exercising only {@link
 * RetryingEmailSenderAdapter}'s own orchestration: retry, metrics, health tracking, audit-facing
 * event publication, and log masking.
 */
class RetryingEmailSenderAdapterTest {

    private static final EmailAddress RECIPIENT = new EmailAddress("someone@example.com");
    private static final EmailMessage MESSAGE = new EmailMessage(
            RECIPIENT, EmailTemplateCategory.WELCOME, new EmailContent("Welcome", "<p>hi</p>", "hi"));
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-11T10:00:00Z"), ZoneOffset.UTC);

    private final List<Object> publishedEvents = new ArrayList<>();
    private final ApplicationEventPublisher eventPublisher = publishedEvents::add;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final EmailProviderHealthTracker healthTracker = new EmailProviderHealthTracker();
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void captureLogs() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(RetryingEmailSenderAdapter.class)).addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(RetryingEmailSenderAdapter.class)).detachAppender(logAppender);
    }

    @Test
    void refusesANegativeRetryCount() {
        assertThatThrownBy(() -> new RetryingEmailSenderAdapter(
                        FakeEmailProviderClient.alwaysSucceeds(), EmailProviderType.RESEND, -1,
                        "noreply@bachatsetu.in", "support@bachatsetu.in",
                        eventPublisher, healthTracker, meterRegistry, CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void succeedsOnTheFirstAttemptWithoutRetrying() {
        FakeEmailProviderClient client = FakeEmailProviderClient.alwaysSucceeds();
        RetryingEmailSenderAdapter adapter = adapter(client, 2);

        EmailSendResult result = adapter.send(MESSAGE);

        assertThat(result.status()).isEqualTo(EmailDeliveryStatus.SENT);
        assertThat(client.attempts).isEqualTo(1);
        assertThat(meterRegistry.counter("email.sent.success", "provider", "RESEND").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("email.sent.failure", "provider", "RESEND").count()).isZero();
        assertThat(meterRegistry.counter("email.retry", "provider", "RESEND").count()).isZero();
        assertThat(meterRegistry.timer("email.duration", "provider", "RESEND").count()).isEqualTo(1);
        assertThat(healthTracker.status()).isEqualTo(EmailProviderHealthStatus.UP);
        assertThat(publishedEvents).singleElement().isInstanceOf(EmailSent.class);
    }

    @Test
    void retriesATransientFailureAndSucceeds() {
        FakeEmailProviderClient client = FakeEmailProviderClient.failsThenSucceeds(
                new EmailProviderException("timeout", true, -1));
        RetryingEmailSenderAdapter adapter = adapter(client, 2);

        EmailSendResult result = adapter.send(MESSAGE);

        assertThat(result.status()).isEqualTo(EmailDeliveryStatus.SENT);
        assertThat(client.attempts).isEqualTo(2);
        assertThat(meterRegistry.counter("email.sent.success", "provider", "RESEND").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("email.retry", "provider", "RESEND").count()).isEqualTo(1.0);
        assertThat(healthTracker.status()).isEqualTo(EmailProviderHealthStatus.UP);
    }

    @Test
    void doesNotRetryANonRetryableFailureEvenWithRetriesRemaining() {
        FakeEmailProviderClient client = FakeEmailProviderClient.alwaysFails(
                new EmailProviderException("invalid request", false, 400));
        RetryingEmailSenderAdapter adapter = adapter(client, 3);

        EmailSendResult result = adapter.send(MESSAGE);

        assertThat(result.status()).isEqualTo(EmailDeliveryStatus.FAILED);
        assertThat(client.attempts).isEqualTo(1);
        assertThat(meterRegistry.counter("email.retry", "provider", "RESEND").count()).isZero();
    }

    @Test
    void neverThrowsAndReturnsAFailedResultOnceRetriesAreExhausted() {
        FakeEmailProviderClient client = FakeEmailProviderClient.alwaysFails(
                new EmailProviderException("RESEND request failed with HTTP 503", true, 503));
        RetryingEmailSenderAdapter adapter = adapter(client, 1);

        EmailSendResult result = adapter.send(MESSAGE);

        assertThat(result.status()).isEqualTo(EmailDeliveryStatus.FAILED);
        assertThat(result.failureReason()).contains("503");
        assertThat(client.attempts).isEqualTo(2);
        assertThat(meterRegistry.counter("email.sent.failure", "provider", "RESEND").count()).isEqualTo(1.0);
        assertThat(healthTracker.consecutiveFailures()).isEqualTo(1);
        assertThat(publishedEvents).singleElement().isInstanceOfSatisfying(EmailSendFailed.class, event -> {
            assertThat(event.category()).isEqualTo(EmailTemplateCategory.WELCOME);
            assertThat(event.provider()).isEqualTo("RESEND");
        });
    }

    @Test
    void reportsProviderDownAfterThreeConsecutiveFailureBatches() {
        FakeEmailProviderClient client = FakeEmailProviderClient.alwaysFails(
                new EmailProviderException("rejected", false, 400));
        RetryingEmailSenderAdapter adapter = adapter(client, 0);

        for (int i = 0; i < 3; i++) {
            assertThat(adapter.send(MESSAGE).status()).isEqualTo(EmailDeliveryStatus.FAILED);
        }

        assertThat(healthTracker.status()).isEqualTo(EmailProviderHealthStatus.DOWN);
    }

    @Test
    void neverLogsTheUnmaskedRecipientAddress() {
        FakeEmailProviderClient client = FakeEmailProviderClient.alwaysSucceeds();
        RetryingEmailSenderAdapter adapter = adapter(client, 0);

        adapter.send(MESSAGE);

        List<String> messages = logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).isNotEmpty();
        assertThat(messages).noneMatch(message -> message.contains(RECIPIENT.value()));
        assertThat(messages).anyMatch(message -> message.contains("so*****@example.com"));
    }

    @Test
    void neverLogsTheUnmaskedRecipientAddressOnFailure() {
        FakeEmailProviderClient client = FakeEmailProviderClient.alwaysFails(
                new EmailProviderException("rejected", false, 400));
        RetryingEmailSenderAdapter adapter = adapter(client, 0);

        adapter.send(MESSAGE);

        List<String> messages = logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).noneMatch(message -> message.contains(RECIPIENT.value()));
        assertThat(messages).anyMatch(message -> message.contains("so*****@example.com"));
    }

    private RetryingEmailSenderAdapter adapter(EmailProviderClient client, int retryCount) {
        return new RetryingEmailSenderAdapter(
                client, EmailProviderType.RESEND, retryCount, "noreply@bachatsetu.in", "support@bachatsetu.in",
                eventPublisher, healthTracker, meterRegistry, CLOCK);
    }

    /**
     * A queue of outcomes to hand back, one per call to {@link #send(EmailProviderMessage)}: an
     * {@link EmailProviderSendResult} is returned, an {@link EmailProviderException} is thrown.
     * The last queued outcome repeats once the queue is drained, so "always succeeds"/"always
     * fails" only need a single queued entry.
     */
    private static final class FakeEmailProviderClient implements EmailProviderClient {
        private final Queue<Object> outcomes;
        private int attempts;

        private FakeEmailProviderClient(Queue<Object> outcomes) {
            this.outcomes = outcomes;
        }

        static FakeEmailProviderClient alwaysSucceeds() {
            Queue<Object> outcomes = new ArrayDeque<>();
            outcomes.add(new EmailProviderSendResult("RESEND", "msg-1", Instant.now()));
            return new FakeEmailProviderClient(outcomes);
        }

        static FakeEmailProviderClient failsThenSucceeds(EmailProviderException firstFailure) {
            Queue<Object> outcomes = new ArrayDeque<>();
            outcomes.add(firstFailure);
            outcomes.add(new EmailProviderSendResult("RESEND", "msg-2", Instant.now()));
            return new FakeEmailProviderClient(outcomes);
        }

        static FakeEmailProviderClient alwaysFails(EmailProviderException failure) {
            Queue<Object> outcomes = new ArrayDeque<>();
            outcomes.add(failure);
            return new FakeEmailProviderClient(outcomes);
        }

        @Override
        public EmailProviderSendResult send(EmailProviderMessage message) {
            attempts++;
            Object outcome = outcomes.size() > 1 ? outcomes.poll() : outcomes.peek();
            if (outcome instanceof EmailProviderException exception) {
                throw exception;
            }
            return (EmailProviderSendResult) outcome;
        }
    }
}
