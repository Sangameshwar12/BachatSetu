package in.bachatsetu.backend.infrastructure.persistence.entity.platform;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.platformoperations.domain.model.AnnouncementSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcements", schema = "platform")
public class AnnouncementJpaEntity extends BaseJpaEntity {

    @NotBlank
    @Column(name = "title", nullable = false, length = 200, updatable = false)
    private String title;

    @NotBlank
    @Column(name = "message", nullable = false, updatable = false)
    private String message;

    @NotNull
    @Column(name = "start_at", nullable = false, updatable = false)
    private Instant startAt;

    @NotNull
    @Column(name = "end_at", nullable = false, updatable = false)
    private Instant endAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10, updatable = false)
    private AnnouncementSeverity severity;

    protected AnnouncementJpaEntity() {
    }

    public AnnouncementJpaEntity(
            UUID id, String title, String message, Instant startAt, Instant endAt, AnnouncementSeverity severity) {
        super(id);
        this.title = title;
        this.message = message;
        this.startAt = startAt;
        this.endAt = endAt;
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public AnnouncementSeverity getSeverity() {
        return severity;
    }
}
