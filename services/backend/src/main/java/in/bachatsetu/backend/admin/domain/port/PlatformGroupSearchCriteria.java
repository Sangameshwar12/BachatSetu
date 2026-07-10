package in.bachatsetu.backend.admin.domain.port;

import in.bachatsetu.backend.admin.domain.model.PlatformGroupStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Cross-tenant search request for savings groups. Every filter is optional: a {@code null} value means "do
 * not filter on this field." Sorted by {@code createdAt} only — no other sort field is required for groups.
 */
public record PlatformGroupSearchCriteria(
        PlatformGroupStatus status,
        Instant createdAfter,
        Instant createdBefore,
        int page,
        int size,
        SortDirection direction) {

    public static final int MAXIMUM_SIZE = 100;

    public PlatformGroupSearchCriteria {
        Objects.requireNonNull(direction, "sort direction must not be null");
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAXIMUM_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAXIMUM_SIZE);
        }
        if (createdAfter != null && createdBefore != null && createdAfter.isAfter(createdBefore)) {
            throw new IllegalArgumentException("createdAfter must not be after createdBefore");
        }
    }
}
