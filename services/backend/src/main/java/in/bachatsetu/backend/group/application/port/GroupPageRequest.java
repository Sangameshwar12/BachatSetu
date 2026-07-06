package in.bachatsetu.backend.group.application.port;

import in.bachatsetu.backend.group.domain.model.GroupStatus;
import java.util.Objects;

/** Tenant-scoped listing request carrying pagination, sorting, and an optional status filter. */
public record GroupPageRequest(
        int page,
        int size,
        GroupSortField sortField,
        SortDirection direction,
        GroupStatus statusFilter) {

    public static final int MAXIMUM_SIZE = 100;

    public GroupPageRequest {
        Objects.requireNonNull(sortField, "sort field must not be null");
        Objects.requireNonNull(direction, "sort direction must not be null");
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAXIMUM_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAXIMUM_SIZE);
        }
    }
}
