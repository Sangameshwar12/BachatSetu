package in.bachatsetu.backend.platformoperations.interfaces.rest.dto;

import java.time.Instant;

public record AnnouncementResponse(
        String announcementId,
        String title,
        String message,
        Instant startAt,
        Instant endAt,
        String severity,
        boolean active) {
}
