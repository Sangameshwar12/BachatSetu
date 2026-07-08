package in.bachatsetu.backend.notification.application.service;

import static in.bachatsetu.backend.notification.application.ApplicationTestFixture.NOW;
import static in.bachatsetu.backend.notification.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.notification.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.command.MarkNotificationDeliveredCommand;
import in.bachatsetu.backend.notification.application.command.MarkNotificationFailedCommand;
import in.bachatsetu.backend.notification.application.exception.NotificationDeliveryFailedException;
import in.bachatsetu.backend.notification.application.exception.NotificationNotFoundException;
import in.bachatsetu.backend.notification.application.mapper.NotificationApplicationMapper;
import in.bachatsetu.backend.notification.application.port.ClockPort;
import in.bachatsetu.backend.notification.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.notification.application.port.EmailSender;
import in.bachatsetu.backend.notification.application.port.InAppNotificationSender;
import in.bachatsetu.backend.notification.application.port.SmsSender;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.application.port.WhatsappSender;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.query.NotificationSummary;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.application.usecase.GetNotificationUseCase;
import in.bachatsetu.backend.notification.application.usecase.ListNotificationsUseCase;
import in.bachatsetu.backend.notification.application.usecase.MarkNotificationDeliveredUseCase;
import in.bachatsetu.backend.notification.application.usecase.MarkNotificationFailedUseCase;
import in.bachatsetu.backend.notification.domain.event.NotificationQueued;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.notification.domain.port.NotificationPage;
import in.bachatsetu.backend.notification.domain.port.NotificationPageRequest;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.notification.domain.port.NotificationSortField;
import in.bachatsetu.backend.notification.domain.port.SortDirection;
import in.bachatsetu.backend.notification.domain.service.NotificationTemplateRenderer;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationApplicationServiceTest {

    private NotificationRepository repository;
    private DomainEventPublisherPort publisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private NotificationApplicationMapper mapper;
    private NotificationTemplateRenderer renderer;
    private EmailSender emailSender;
    private SmsSender smsSender;
    private WhatsappSender whatsappSender;
    private InAppNotificationSender inAppNotificationSender;
    private CreateAuditEntryUseCase createAuditEntry;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        publisher = mock(DomainEventPublisherPort.class);
        clock = () -> NOW;
        transaction = directTransaction();
        mapper = new NotificationApplicationMapper();
        renderer = new NotificationTemplateRenderer();
        emailSender = mock(EmailSender.class);
        smsSender = mock(SmsSender.class);
        whatsappSender = mock(WhatsappSender.class);
        inAppNotificationSender = mock(InAppNotificationSender.class);
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
        when(emailSender.send(any(), any())).thenReturn("EMAIL-provider-id");
        when(smsSender.send(any(), any())).thenReturn("SMS-provider-id");
        when(whatsappSender.send(any(), any())).thenReturn("WHATSAPP-provider-id");
        when(inAppNotificationSender.send(any(), any())).thenReturn("IN_APP-provider-id");
    }

    @Test
    void createsDispatchesSavesAndPublishesANotification() {
        CreateNotificationCommand command = createCommand();
        CreateNotificationUseCase service = createService();

        NotificationResult result = service.execute(command);

        assertThat(result.status()).isEqualTo("SENT");
        assertThat(result.channel()).isEqualTo("EMAIL");
        assertThat(result.body()).contains("Asha");
        verify(repository).save(any(Notification.class));
        verify(emailSender).send(any(), any());
        assertPublishedEventsInclude(NotificationQueued.class);
        verify(createAuditEntry).execute(any());
    }

    @Test
    void dispatchesOverTheRequestedChannel() {
        CreateNotificationUseCase service = createService();

        service.execute(new CreateNotificationCommand(
                AggregateId.newId(), AggregateId.newId(), "+919876543210", NotificationChannel.SMS,
                NotificationCategory.SECURITY_ALERT, java.util.Map.of("memberName", "Ravi"), AggregateId.newId()));

        verify(smsSender).send(any(), any());
        verify(emailSender, never()).send(any(), any());
    }

    @Test
    void dispatchesOverThePushChannel() {
        CreateNotificationUseCase service = createService();

        service.execute(new CreateNotificationCommand(
                AggregateId.newId(), AggregateId.newId(), "device-token", NotificationChannel.PUSH,
                NotificationCategory.GROUP_UPDATE, java.util.Map.of("memberName", "Sita", "groupName", "Bhishi"),
                AggregateId.newId()));

        verify(inAppNotificationSender).send(any(), any());
    }

    @Test
    void wrapsAChannelFailureAsADeliveryFailedException() {
        when(emailSender.send(any(), any())).thenThrow(new RuntimeException("provider down"));
        CreateNotificationUseCase service = createService();

        assertThatThrownBy(() -> service.execute(createCommand()))
                .isInstanceOf(NotificationDeliveryFailedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void retrievesTenantScopedNotification() {
        AggregateId tenantId = AggregateId.newId();
        Notification notification = newNotification();
        when(repository.findById(tenantId, notification.id())).thenReturn(Optional.of(notification));
        GetNotificationUseCase service = new GetNotificationApplicationService(repository, transaction, mapper);

        NotificationResult result = service.execute(tenantId, notification.id());

        assertThat(result.notificationId()).isEqualTo(notification.id().value());
    }

    @Test
    void tenantScopedLookupHidesNotificationsFromOtherTenants() {
        AggregateId tenantId = AggregateId.newId();
        Notification notification = newNotification();
        when(repository.findById(tenantId, notification.id())).thenReturn(Optional.empty());
        GetNotificationUseCase service = new GetNotificationApplicationService(repository, transaction, mapper);

        assertThatThrownBy(() -> service.execute(tenantId, notification.id()))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void listsTenantScopedNotificationSummaries() {
        AggregateId tenantId = AggregateId.newId();
        Notification first = newNotification();
        Notification second = newNotification();
        NotificationPageRequest pageRequest =
                new NotificationPageRequest(0, 20, NotificationSortField.CREATED_AT, SortDirection.ASC);
        when(repository.findPage(tenantId, pageRequest))
                .thenReturn(new NotificationPage<>(List.of(first, second), 0, 20, 2));
        ListNotificationsUseCase service = new ListNotificationsApplicationService(repository, transaction, mapper);

        NotificationPage<NotificationSummary> page = service.execute(tenantId, pageRequest);

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThatThrownBy(() -> page.content().add(mapper.toSummary(first)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void marksASentNotificationAsDelivered() {
        AggregateId tenantId = AggregateId.newId();
        Notification notification = newNotification();
        notification.startDelivery(AggregateId.newId(), NOW);
        notification.markSent("provider-msg-1", AggregateId.newId(), NOW.plusSeconds(1));
        when(repository.findById(tenantId, notification.id())).thenReturn(Optional.of(notification));
        ClockPort laterClock = () -> NOW.plusSeconds(10);
        MarkNotificationDeliveredUseCase service = new MarkNotificationDeliveredApplicationService(
                repository, publisher, laterClock, transaction, mapper);

        NotificationResult result = service.execute(
                new MarkNotificationDeliveredCommand(tenantId, notification.id(), AggregateId.newId()));

        assertThat(result.status()).isEqualTo("DELIVERED");
        verify(repository).save(notification);
    }

    @Test
    void markDeliveredPropagatesNotFoundForAMissingNotification() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId notificationId = AggregateId.newId();
        when(repository.findById(tenantId, notificationId)).thenReturn(Optional.empty());
        MarkNotificationDeliveredUseCase service = new MarkNotificationDeliveredApplicationService(
                repository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(
                        new MarkNotificationDeliveredCommand(tenantId, notificationId, AggregateId.newId())))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void marksASendingNotificationAsFailed() {
        AggregateId tenantId = AggregateId.newId();
        Notification notification = newNotification();
        notification.startDelivery(AggregateId.newId(), NOW);
        when(repository.findById(tenantId, notification.id())).thenReturn(Optional.of(notification));
        ClockPort laterClock = () -> NOW.plusSeconds(10);
        MarkNotificationFailedUseCase service = new MarkNotificationFailedApplicationService(
                repository, publisher, laterClock, transaction, mapper);

        NotificationResult result = service.execute(new MarkNotificationFailedCommand(
                tenantId, notification.id(), "provider-timeout", AggregateId.newId()));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.failureReason()).isEqualTo("provider-timeout");
    }

    @Test
    void markFailedPropagatesNotFoundForAMissingNotification() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId notificationId = AggregateId.newId();
        when(repository.findById(tenantId, notificationId)).thenReturn(Optional.empty());
        MarkNotificationFailedUseCase service = new MarkNotificationFailedApplicationService(
                repository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(
                        new MarkNotificationFailedCommand(tenantId, notificationId, "code", AggregateId.newId())))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void rejectsNullUseCaseInputs() {
        assertThatThrownBy(() -> createService().execute(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetNotificationApplicationService(repository, transaction, mapper)
                        .execute(null, AggregateId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetNotificationApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListNotificationsApplicationService(repository, transaction, mapper)
                        .execute(null, new NotificationPageRequest(
                                0, 20, NotificationSortField.CREATED_AT, SortDirection.ASC)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListNotificationsApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MarkNotificationDeliveredApplicationService(
                        repository, publisher, clock, transaction, mapper)
                        .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MarkNotificationFailedApplicationService(
                        repository, publisher, clock, transaction, mapper)
                        .execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new GetNotificationApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetNotificationApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetNotificationApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListNotificationsApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListNotificationsApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListNotificationsApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateNotificationApplicationService(
                        null, publisher, clock, transaction, mapper, renderer,
                        emailSender, smsSender, whatsappSender, inAppNotificationSender, createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateNotificationApplicationService(
                        repository, publisher, null, transaction, mapper, renderer,
                        emailSender, smsSender, whatsappSender, inAppNotificationSender, createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateNotificationApplicationService(
                        repository, publisher, clock, transaction, mapper, renderer,
                        emailSender, smsSender, whatsappSender, inAppNotificationSender, null))
                .isInstanceOf(NullPointerException.class);
    }

    private CreateNotificationUseCase createService() {
        return new CreateNotificationApplicationService(
                repository, publisher, clock, transaction, mapper, renderer,
                emailSender, smsSender, whatsappSender, inAppNotificationSender, createAuditEntry);
    }

    private Notification newNotification() {
        return Notification.queue(
                AggregateId.newId(), AggregateId.newId(),
                new NotificationRecipient(AggregateId.newId(), "member@example.com"),
                NotificationChannel.EMAIL, NotificationCategory.VERIFICATION,
                new NotificationContent("Account verification", "Please verify your account."),
                NOW, AggregateId.newId(), NOW);
    }

    @SafeVarargs
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertPublishedEventsInclude(Class<? extends DomainEvent>... eventTypes) {
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(publisher).publish(captor.capture());
        for (Class<? extends DomainEvent> eventType : eventTypes) {
            assertThat(captor.getValue()).anyMatch(eventType::isInstance);
        }
    }
}
