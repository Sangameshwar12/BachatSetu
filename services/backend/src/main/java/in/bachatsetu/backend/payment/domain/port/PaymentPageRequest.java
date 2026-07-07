package in.bachatsetu.backend.payment.domain.port;

import java.util.Objects;

/** Tenant-scoped listing request carrying pagination and sorting. */
public record PaymentPageRequest(int page, int size, PaymentSortField sortField, SortDirection direction) {

    public static final int MAXIMUM_SIZE = 100;

    public PaymentPageRequest {
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
