package in.bachatsetu.backend.support.domain.port;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.SortDirection;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.model.TicketStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Cross-tenant search request for support tickets. Every filter is optional: a {@code null} value means
 * "do not filter on this field."
 */
public record SupportTicketSearchCriteria(
        TicketStatus status,
        TicketPriority priority,
        TicketCategory category,
        AggregateId tenantId,
        AggregateId raisedBy,
        Instant createdAfter,
        Instant createdBefore,
        int page,
        int size,
        SortDirection direction) {

    public static final int MAXIMUM_SIZE = 100;

    public SupportTicketSearchCriteria {
        Objects.requireNonNull(direction, "direction must not be null");
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
