package in.bachatsetu.backend.platformoperations.application.command;

import in.bachatsetu.backend.platformoperations.domain.model.AnnouncementSeverity;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public record PublishAnnouncementCommand(
        String title,
        String message,
        Instant startAt,
        Instant endAt,
        AnnouncementSeverity severity,
        AggregateId actorId) {

    public PublishAnnouncementCommand {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(startAt, "startAt must not be null");
        Objects.requireNonNull(endAt, "endAt must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
    }
}
