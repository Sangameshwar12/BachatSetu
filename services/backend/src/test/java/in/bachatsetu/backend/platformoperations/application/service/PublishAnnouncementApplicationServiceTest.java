package in.bachatsetu.backend.platformoperations.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.platformoperations.application.command.PublishAnnouncementCommand;
import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.platformoperations.domain.model.Announcement;
import in.bachatsetu.backend.platformoperations.domain.model.AnnouncementSeverity;
import in.bachatsetu.backend.platformoperations.domain.port.AnnouncementRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class PublishAnnouncementApplicationServiceTest {

    private static final Instant START = Instant.parse("2026-07-10T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-20T00:00:00Z");

    private final AnnouncementRepository repository = mock(AnnouncementRepository.class);
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final PlatformOperationsApplicationMapper mapper = new PlatformOperationsApplicationMapper();
    private final PublishAnnouncementApplicationService service = new PublishAnnouncementApplicationService(
            repository, eventPublisher, createAuditEntry, clock, transaction, mapper);

    @Test
    void publishesAndSavesAnAnnouncement() {
        when(clock.now()).thenReturn(START);

        AnnouncementResult result = service.execute(new PublishAnnouncementCommand(
                "Maintenance", "Downtime expected", START, END, AnnouncementSeverity.WARNING, AggregateId.newId()));

        assertThat(result.title()).isEqualTo("Maintenance");
        assertThat(result.active()).isTrue();
        verify(repository).save(any(Announcement.class));
        verify(eventPublisher).publish(any());
        verify(createAuditEntry).execute(any());
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
