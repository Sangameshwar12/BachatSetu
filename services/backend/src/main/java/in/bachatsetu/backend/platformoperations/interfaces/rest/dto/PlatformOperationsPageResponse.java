package in.bachatsetu.backend.platformoperations.interfaces.rest.dto;

import java.util.List;
import java.util.Objects;

/** Presentation wrapper for one page of results, decoupled from any persistence paging type. */
public record PlatformOperationsPageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages, boolean hasNext,
        boolean hasPrevious) {

    public PlatformOperationsPageResponse {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
    }
}
