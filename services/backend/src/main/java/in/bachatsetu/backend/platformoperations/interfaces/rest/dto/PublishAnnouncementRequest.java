package in.bachatsetu.backend.platformoperations.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record PublishAnnouncementRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 4000) String message,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @NotNull String severity) {
}
