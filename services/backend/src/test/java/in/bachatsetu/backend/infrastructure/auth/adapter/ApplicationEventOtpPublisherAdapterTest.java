package in.bachatsetu.backend.infrastructure.auth.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import in.bachatsetu.backend.auth.application.event.OtpVerified;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ApplicationEventOtpPublisherAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void publishesAnOtpEventThroughTheSpringEventBus() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ApplicationEventOtpPublisherAdapter adapter = new ApplicationEventOtpPublisherAdapter(publisher);
        OtpVerified event = new OtpVerified(
                UUID.randomUUID(), AggregateId.newId(), UserId.newId(), OtpPurpose.SIGN_IN, NOW);

        adapter.publish(event);

        verify(publisher).publishEvent(event);
    }

    @Test
    void rejectsANullEvent() {
        ApplicationEventOtpPublisherAdapter adapter =
                new ApplicationEventOtpPublisherAdapter(mock(ApplicationEventPublisher.class));

        assertThatThrownBy(() -> adapter.publish(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullPublisher() {
        assertThatThrownBy(() -> new ApplicationEventOtpPublisherAdapter(null))
                .isInstanceOf(NullPointerException.class);
    }
}
