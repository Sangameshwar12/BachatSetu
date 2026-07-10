package in.bachatsetu.backend.platformoperations.application.query;

import in.bachatsetu.backend.platformoperations.domain.model.AnnouncementSeverity;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;

public record AnnouncementResult(
        AggregateId announcementId,
        String title,
        String message,
        Instant startAt,
        Instant endAt,
        AnnouncementSeverity severity,
        boolean active) {
}
