package in.bachatsetu.backend.admin.domain.port;

import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Cross-tenant search request for platform users. Every filter is optional: a {@code null} value means "do
 * not filter on this field."
 */
public record PlatformUserSearchCriteria(
        PlatformUserStatus status,
        String email,
        String phoneNumber,
        Instant createdAfter,
        Instant createdBefore,
        int page,
        int size,
        PlatformUserSortField sortField,
        SortDirection direction) {

    public static final int MAXIMUM_SIZE = 100;

    public PlatformUserSearchCriteria {
        Objects.requireNonNull(sortField, "sort field must not be null");
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
