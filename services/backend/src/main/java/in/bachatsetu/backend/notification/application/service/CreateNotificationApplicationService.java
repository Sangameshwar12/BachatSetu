package in.bachatsetu.backend.notification.application.service;

import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.exception.NotificationDeliveryFailedException;
import in.bachatsetu.backend.notification.application.mapper.NotificationApplicationMapper;
import in.bachatsetu.backend.notification.application.port.ClockPort;
import in.bachatsetu.backend.notification.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.notification.application.port.EmailSender;
import in.bachatsetu.backend.notification.application.port.InAppNotificationSender;
import in.bachatsetu.backend.notification.application.port.SmsSender;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.application.port.WhatsappSender;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.notification.domain.model.NotificationTemplate;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.notification.domain.service.NotificationTemplateCatalog;
import in.bachatsetu.backend.notification.domain.service.NotificationTemplateRenderer;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates notification creation and its synchronous, in-request channel dispatch.
 *
 * <p>Constructs the aggregate via {@link Notification#queue} directly rather than the pre-existing
 * {@code NotificationFactory}: the factory reads its own internal {@code Clock} to derive {@code queuedAt},
 * a value independent of and always fractionally later than any {@code scheduledAt} an application-layer
 * caller could capture beforehand, which makes {@code Notification.queue}'s
 * {@code scheduledAt.isBefore(queuedAt)} guard reject "send immediately" requests essentially every time.
 * Calling the aggregate's own public factory method directly, with one {@link ClockPort#now()} reading
 * reused for both {@code scheduledAt} and {@code queuedAt}, sidesteps that race without changing any
 * existing class.
 */
public final class CreateNotificationApplicationService implements CreateNotificationUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final NotificationTemplateRenderer renderer;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final WhatsappSender whatsappSender;
    private final InAppNotificationSender inAppNotificationSender;
    private final NotificationApplicationSupport support;

    public CreateNotificationApplicationService(
            NotificationRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            NotificationApplicationMapper mapper,
            NotificationTemplateRenderer renderer,
            EmailSender emailSender,
            SmsSender smsSender,
            WhatsappSender whatsappSender,
            InAppNotificationSender inAppNotificationSender) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer must not be null");
        this.emailSender = Objects.requireNonNull(emailSender, "email sender must not be null");
        this.smsSender = Objects.requireNonNull(smsSender, "sms sender must not be null");
        this.whatsappSender = Objects.requireNonNull(whatsappSender, "whatsapp sender must not be null");
        this.inAppNotificationSender =
                Objects.requireNonNull(inAppNotificationSender, "in-app sender must not be null");
        this.support = new NotificationApplicationSupport(
                Objects.requireNonNull(repository, "repository must not be null"), eventPublisher, mapper);
    }

    @Override
    public NotificationResult execute(CreateNotificationCommand command) {
        Objects.requireNonNull(command, "create command must not be null");
        return transaction.execute(() -> create(command));
    }

    private NotificationResult create(CreateNotificationCommand command) {
        NotificationTemplate template = NotificationTemplateCatalog.templateFor(command.category());
        NotificationContent content = renderer.render(template, command.placeholders());
        NotificationRecipient recipient = new NotificationRecipient(command.recipientUserId(), command.destination());
        Instant now = clock.now();

        Notification notification = Notification.queue(
                AggregateId.newId(),
                command.tenantId(),
                recipient,
                command.channel(),
                command.category(),
                content,
                now,
                command.actorId(),
                now);

        notification.startDelivery(command.actorId(), now);
        String providerMessageId = dispatch(command.channel(), recipient, content);
        notification.markSent(providerMessageId, command.actorId(), clock.now());
        return support.saveAndPublish(notification);
    }

    private String dispatch(NotificationChannel channel, NotificationRecipient recipient, NotificationContent content) {
        try {
            return switch (channel) {
                case EMAIL -> emailSender.send(recipient, content);
                case SMS -> smsSender.send(recipient, content);
                case WHATSAPP -> whatsappSender.send(recipient, content);
                case PUSH -> inAppNotificationSender.send(recipient, content);
            };
        } catch (RuntimeException exception) {
            throw new NotificationDeliveryFailedException("failed to dispatch notification", exception);
        }
    }
}
