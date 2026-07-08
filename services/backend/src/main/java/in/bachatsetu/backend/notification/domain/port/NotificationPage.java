package in.bachatsetu.backend.notification.domain.port;

import java.util.List;
import java.util.Objects;

/** Database-paginated collection with metadata derived from a single source of truth. */
public record NotificationPage<T>(List<T> content, int page, int size, long totalElements) {

    public NotificationPage {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("total elements must not be negative");
        }
    }

    public int totalPages() {
        return totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return (long) (page + 1) * size < totalElements;
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
