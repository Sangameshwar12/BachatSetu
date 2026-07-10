package in.bachatsetu.backend.platformoperations.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.platformoperations.domain.model.Announcement;
import in.bachatsetu.backend.platformoperations.domain.model.AnnouncementSeverity;
import in.bachatsetu.backend.platformoperations.domain.port.AnnouncementRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ListActiveAnnouncementsApplicationServiceTest {

    private static final Instant START = Instant.parse("2026-07-10T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-20T00:00:00Z");

    private final AnnouncementRepository repository = mock(AnnouncementRepository.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final PlatformOperationsApplicationMapper mapper = new PlatformOperationsApplicationMapper();
    private final ListActiveAnnouncementsApplicationService service =
            new ListActiveAnnouncementsApplicationService(repository, clock, transaction, mapper);

    @Test
    void listsOnlyCurrentlyActiveAnnouncements() {
        Announcement announcement = Announcement.publish(
                AggregateId.newId(), "Title", "Message", START, END, AnnouncementSeverity.CRITICAL,
                AggregateId.newId(), START);
        when(clock.now()).thenReturn(START.plusSeconds(60));
        when(repository.findActive(START.plusSeconds(60))).thenReturn(List.of(announcement));

        List<AnnouncementResult> result = service.execute();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).active()).isTrue();
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
