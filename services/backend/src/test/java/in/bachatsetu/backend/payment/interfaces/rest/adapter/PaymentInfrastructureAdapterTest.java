package in.bachatsetu.backend.payment.interfaces.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

class PaymentInfrastructureAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    @Test
    void systemClockDelegatesToInjectedJavaClock() {
        SystemPaymentClockAdapter adapter = new SystemPaymentClockAdapter(Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(adapter.now()).isEqualTo(NOW);
    }

    @Test
    void transactionAdapterExecutesOperationThroughTransactionTemplate() {
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<String> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        SpringPaymentTransactionAdapter adapter = new SpringPaymentTransactionAdapter(template);

        String result = adapter.execute(() -> "completed");

        assertThat(result).isEqualTo("completed");
        verify(template, times(1)).execute(any());
    }

    @Test
    void eventPublisherAdapterPublishesEveryDomainEvent() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ApplicationEventPaymentEventPublisherAdapter adapter =
                new ApplicationEventPaymentEventPublisherAdapter(publisher);
        DomainEvent first = new RecordedEvent(UUID.randomUUID());
        DomainEvent second = new RecordedEvent(UUID.randomUUID());

        adapter.publish(List.of(first, second));

        verify(publisher).publishEvent(first);
        verify(publisher).publishEvent(second);
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
