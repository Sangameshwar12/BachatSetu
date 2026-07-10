package in.bachatsetu.backend.platformoperations.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.platformoperations.domain.exception.PlatformOperationsDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AnnouncementTest {

    private static final Instant START = Instant.parse("2026-07-10T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-20T00:00:00Z");

    @Test
    void publishesAnAnnouncementAndRegistersAnEvent() {
        Announcement announcement = Announcement.publish(
                AggregateId.newId(), "Scheduled maintenance", "The app will be down for 1 hour", START, END,
                AnnouncementSeverity.WARNING, AggregateId.newId(), START);

        assertThat(announcement.title()).isEqualTo("Scheduled maintenance");
        assertThat(announcement.severity()).isEqualTo(AnnouncementSeverity.WARNING);
        assertThat(announcement.domainEvents()).hasSize(1);
    }

    @Test
    void rejectsAnEndDateBeforeTheStartDate() {
        assertThatThrownBy(() -> Announcement.publish(
                        AggregateId.newId(), "Title", "Message", END, START, AnnouncementSeverity.INFO,
                        AggregateId.newId(), START))
                .isInstanceOf(PlatformOperationsDomainException.class);
    }

    @Test
    void isActiveWithinTheInclusiveWindow() {
        Announcement announcement = Announcement.publish(
                AggregateId.newId(), "Title", "Message", START, END, AnnouncementSeverity.INFO, AggregateId.newId(),
                START);

        assertThat(announcement.isActive(START)).isTrue();
        assertThat(announcement.isActive(END)).isTrue();
        assertThat(announcement.isActive(START.plusSeconds(60))).isTrue();
    }

    @Test
    void isNotActiveOutsideTheWindow() {
        Announcement announcement = Announcement.publish(
                AggregateId.newId(), "Title", "Message", START, END, AnnouncementSeverity.INFO, AggregateId.newId(),
                START);

        assertThat(announcement.isActive(START.minusSeconds(1))).isFalse();
        assertThat(announcement.isActive(END.plusSeconds(1))).isFalse();
    }
}
