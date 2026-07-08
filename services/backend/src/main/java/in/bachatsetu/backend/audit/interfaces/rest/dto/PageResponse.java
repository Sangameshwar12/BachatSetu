package in.bachatsetu.backend.audit.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/** Presentation wrapper for one page of results, decoupled from any persistence paging type. */
public record PageResponse<T>(

        @Schema(description = "Page content") List<T> content,
        @Schema(description = "Zero-based page index") int page,
        @Schema(description = "Page size") int size,
        @Schema(description = "Total number of elements across every page") long totalElements,
        @Schema(description = "Total number of pages") int totalPages,
        @Schema(description = "Whether a next page exists") boolean hasNext,
        @Schema(description = "Whether a previous page exists") boolean hasPrevious) {

    public PageResponse {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
    }
}
