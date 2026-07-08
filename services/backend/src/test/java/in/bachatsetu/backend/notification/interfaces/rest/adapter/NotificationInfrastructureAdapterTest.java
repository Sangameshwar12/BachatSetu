package in.bachatsetu.backend.notification.interfaces.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class NotificationInfrastructureAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void clockAdapterDelegatesToTheInjectedClock() {
        SystemNotificationClockAdapter adapter =
                new SystemNotificationClockAdapter(Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(adapter.now()).isEqualTo(NOW);
    }

    @Test
    void transactionAdapterExecutesOperationThroughTransactionTemplate() {
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<String> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        SpringNotificationTransactionAdapter adapter = new SpringNotificationTransactionAdapter(template);

        String result = adapter.execute(() -> "completed");

        assertThat(result).isEqualTo("completed");
        verify(template, times(1)).execute(any());
    }

    @Test
    void eventPublisherAdapterPublishesEveryDomainEvent() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ApplicationEventNotificationEventPublisherAdapter adapter =
                new ApplicationEventNotificationEventPublisherAdapter(publisher);
        DomainEvent first = new RecordedEvent(UUID.randomUUID());
        DomainEvent second = new RecordedEvent(UUID.randomUUID());

        adapter.publish(List.of(first, second));

        verify(publisher).publishEvent(first);
        verify(publisher).publishEvent(second);
    }

    @Test
    void emailSenderReturnsAProviderMessageId() {
        LoggingEmailSenderAdapter adapter = new LoggingEmailSenderAdapter();

        String providerMessageId = adapter.send(recipient(), content());

        assertThat(providerMessageId).startsWith("EMAIL-");
    }

    @Test
    void smsSenderReturnsAProviderMessageId() {
        LoggingSmsSenderAdapter adapter = new LoggingSmsSenderAdapter();

        String providerMessageId = adapter.send(recipient(), content());

        assertThat(providerMessageId).startsWith("SMS-");
    }

    @Test
    void whatsappSenderReturnsAProviderMessageId() {
        LoggingWhatsappSenderAdapter adapter = new LoggingWhatsappSenderAdapter();

        String providerMessageId = adapter.send(recipient(), content());

        assertThat(providerMessageId).startsWith("WHATSAPP-");
    }

    @Test
    void inAppSenderReturnsAProviderMessageId() {
        LoggingInAppNotificationSenderAdapter adapter = new LoggingInAppNotificationSenderAdapter();

        String providerMessageId = adapter.send(recipient(), content());

        assertThat(providerMessageId).startsWith("IN_APP-");
    }

    @Test
    void destinationMaskingHidesTheMiddleOfLongDestinations() {
        assertThat(NotificationDestinationMasking.mask("member@example.com")).isEqualTo("me**************om");
        assertThat(NotificationDestinationMasking.mask("ab")).isEqualTo("**");
    }

    private NotificationRecipient recipient() {
        return new NotificationRecipient(AggregateId.newId(), "member@example.com");
    }

    private NotificationContent content() {
        return new NotificationContent("Account verification", "Please verify your account.");
    }

    private record RecordedEvent(UUID eventId) implements DomainEvent {

        @Override
        public UUID eventId() {
            return eventId;
        }

        @Override
        public AggregateId aggregateId() {
            return AggregateId.newId();
        }

        @Override
        public Instant occurredAt() {
            return NOW;
        }
    }
}
