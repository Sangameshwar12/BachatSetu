package in.bachatsetu.backend.receipt.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/** Offset-paginated collection returned by list endpoints. */
public record PageResponse<T>(

        @Schema(description = "Page content") List<T> content,
        @Schema(description = "Zero-based page index", example = "0") int page,
        @Schema(description = "Requested page size", example = "20") int size,
        @Schema(description = "Total number of elements across all pages") long totalElements,
        @Schema(description = "Total number of pages") int totalPages,
        @Schema(description = "Whether a next page exists") boolean hasNext,
        @Schema(description = "Whether a previous page exists") boolean hasPrevious) {

    public PageResponse {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
    }
}
