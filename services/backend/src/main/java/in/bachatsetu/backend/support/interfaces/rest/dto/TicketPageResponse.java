package in.bachatsetu.backend.support.interfaces.rest.dto;

import java.util.List;
import java.util.Objects;

/** Presentation wrapper for one page of ticket results, decoupled from any persistence paging type. */
public record TicketPageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages, boolean hasNext,
        boolean hasPrevious) {

    public TicketPageResponse {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
    }
}
