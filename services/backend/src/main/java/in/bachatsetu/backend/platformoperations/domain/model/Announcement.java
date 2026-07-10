package in.bachatsetu.backend.platformoperations.domain.model;

import in.bachatsetu.backend.platformoperations.domain.event.AnnouncementPublished;
import in.bachatsetu.backend.platformoperations.domain.exception.PlatformOperationsDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A platform-wide announcement, active for the inclusive window {@code [startAt, endAt]}. */
public final class Announcement extends BaseAggregateRoot {

    private final String title;
    private final String message;
    private final Instant startAt;
    private final Instant endAt;
    private final AnnouncementSeverity severity;

    private Announcement(
            AggregateId id,
            String title,
            String message,
            Instant startAt,
            Instant endAt,
            AnnouncementSeverity severity,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.title = requireNonBlank(title, "title");
        this.message = requireNonBlank(message, "message");
        this.startAt = Objects.requireNonNull(startAt, "startAt must not be null");
        this.endAt = Objects.requireNonNull(endAt, "endAt must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        if (endAt.isBefore(startAt)) {
            throw new PlatformOperationsDomainException("endAt must not precede startAt");
        }
    }

    public static Announcement publish(
            AggregateId id,
            String title,
            String message,
            Instant startAt,
            Instant endAt,
            AnnouncementSeverity severity,
            AggregateId actorId,
            Instant publishedAt) {
        Announcement announcement = new Announcement(
                id, title, message, startAt, endAt, severity, AuditInfo.createdBy(actorId, publishedAt), 0);
        announcement.registerEvent(new AnnouncementPublished(UUID.randomUUID(), id, publishedAt));
        return announcement;
    }

    public static Announcement rehydrate(
            AggregateId id,
            String title,
            String message,
            Instant startAt,
            Instant endAt,
            AnnouncementSeverity severity,
            AuditInfo auditInfo,
            long version) {
        return new Announcement(id, title, message, startAt, endAt, severity, auditInfo, version);
    }

    public boolean isActive(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return !now.isBefore(startAt) && !now.isAfter(endAt);
    }

    public String title() {
        return title;
    }

    public String message() {
        return message;
    }

    public Instant startAt() {
        return startAt;
    }

    public Instant endAt() {
        return endAt;
    }

    public AnnouncementSeverity severity() {
        return severity;
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
