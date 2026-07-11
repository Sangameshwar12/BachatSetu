package in.bachatsetu.backend.infrastructure.email.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import in.bachatsetu.backend.email.domain.model.EmailAddress;
import in.bachatsetu.backend.email.domain.model.EmailContent;
import in.bachatsetu.backend.email.domain.model.EmailDeliveryStatus;
import in.bachatsetu.backend.email.domain.model.EmailMessage;
import in.bachatsetu.backend.email.domain.model.EmailSendResult;
import in.bachatsetu.backend.email.domain.model.EmailTemplateCategory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LoggingEmailSenderAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-11T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID FIXED_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void captureLogs() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(LoggingEmailSenderAdapter.class)).addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(LoggingEmailSenderAdapter.class)).detachAppender(logAppender);
    }

    @Test
    void returnsASentResultWithAFabricatedMessageId() {
        LoggingEmailSenderAdapter adapter = new LoggingEmailSenderAdapter(CLOCK, () -> FIXED_ID);
        EmailMessage message = new EmailMessage(
                new EmailAddress("someone@example.com"), EmailTemplateCategory.WELCOME,
                new EmailContent("Welcome", "<p>hi</p>", "hi"));

        EmailSendResult result = adapter.send(message);

        assertThat(result.status()).isEqualTo(EmailDeliveryStatus.SENT);
        assertThat(result.provider()).isEqualTo("LOGGING");
        assertThat(result.providerMessageId()).isEqualTo("EMAIL-" + FIXED_ID);
    }

    @Test
    void neverLogsTheUnmaskedRecipientAddress() {
        LoggingEmailSenderAdapter adapter = new LoggingEmailSenderAdapter(CLOCK, () -> FIXED_ID);
        EmailMessage message = new EmailMessage(
                new EmailAddress("someone@example.com"), EmailTemplateCategory.WELCOME,
                new EmailContent("Welcome", "<p>hi</p>", "hi"));

        adapter.send(message);

        List<String> messages = logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).isNotEmpty();
        assertThat(messages).noneMatch(entry -> entry.contains("someone@example.com"));
        assertThat(messages).anyMatch(entry -> entry.contains("so*****@example.com"));
    }
}
