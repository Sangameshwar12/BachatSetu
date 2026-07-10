package in.bachatsetu.backend.infrastructure.auth.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import in.bachatsetu.backend.auth.domain.event.UserActivated;
import in.bachatsetu.backend.auth.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ApplicationEventDomainEventPublisherAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    @Test
    void publishesEveryEventThroughTheSpringEventBus() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ApplicationEventDomainEventPublisherAdapter adapter =
                new ApplicationEventDomainEventPublisherAdapter(publisher);
        UserActivated event = new UserActivated(UUID.randomUUID(), UserId.newId(), NOW);

        adapter.publish(List.of(event));

        verify(publisher).publishEvent(event);
    }

    @Test
    void rejectsNullEvents() {
        ApplicationEventDomainEventPublisherAdapter adapter =
                new ApplicationEventDomainEventPublisherAdapter(mock(ApplicationEventPublisher.class));

        assertThatThrownBy(() -> adapter.publish(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullPublisher() {
        assertThatThrownBy(() -> new ApplicationEventDomainEventPublisherAdapter(null))
                .isInstanceOf(NullPointerException.class);
    }
}
