package in.bachatsetu.backend.infrastructure.email.adapter;

import in.bachatsetu.backend.email.application.port.EmailSenderPort;
import in.bachatsetu.backend.email.domain.model.EmailAddress;
import in.bachatsetu.backend.email.domain.model.EmailDeliveryStatus;
import in.bachatsetu.backend.email.domain.model.EmailMessage;
import in.bachatsetu.backend.email.domain.model.EmailSendResult;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Local-only sender that confirms generation without logging email content or the recipient in full. */
public final class LoggingEmailSenderAdapter implements EmailSenderPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingEmailSenderAdapter.class);
    private static final int VISIBLE_LOCAL_PART_CHARS = 2;

    private final Clock clock;
    private final Supplier<UUID> correlationIdSupplier;

    public LoggingEmailSenderAdapter(Clock clock, Supplier<UUID> correlationIdSupplier) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.correlationIdSupplier = Objects.requireNonNull(
                correlationIdSupplier, "correlation ID supplier must not be null");
    }

    @Override
    public EmailSendResult send(EmailMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        UUID correlationId = Objects.requireNonNull(
                correlationIdSupplier.get(), "correlation ID must not be null");
        String providerMessageId = "EMAIL-" + correlationId;
        LOGGER.info(
                "Email generated successfully. destination={} category={} providerMessageId={} timestamp={}",
                mask(message.to()),
                message.category(),
                providerMessageId,
                clock.instant());
        return new EmailSendResult(
                EmailDeliveryStatus.SENT, "LOGGING", providerMessageId, clock.instant(), null);
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
}
