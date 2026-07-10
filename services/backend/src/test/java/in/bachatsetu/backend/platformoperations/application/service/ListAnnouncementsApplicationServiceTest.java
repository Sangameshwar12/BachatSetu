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
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ListAnnouncementsApplicationServiceTest {

    private static final Instant START = Instant.parse("2026-07-10T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-20T00:00:00Z");

    private final AnnouncementRepository repository = mock(AnnouncementRepository.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = new StubTransactionPort();
    private final PlatformOperationsApplicationMapper mapper = new PlatformOperationsApplicationMapper();
    private final ListAnnouncementsApplicationService service =
            new ListAnnouncementsApplicationService(repository, clock, transaction, mapper);

    @Test
    void listsEveryAnnouncement() {
        Announcement announcement = Announcement.publish(
                AggregateId.newId(), "Title", "Message", START, END, AnnouncementSeverity.INFO, AggregateId.newId(),
                START);
        when(clock.now()).thenReturn(START);
        when(repository.findAll(new PageQuery(0, 20))).thenReturn(new Page<>(List.of(announcement), 0, 20, 1));

        Page<AnnouncementResult> result = service.execute(new PageQuery(0, 20));

        assertThat(result.content()).hasSize(1);
    }

    private static final class StubTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
