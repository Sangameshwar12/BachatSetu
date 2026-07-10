package in.bachatsetu.backend.admin.domain.analytics.model;

import java.util.Objects;

/** One category's share of a distribution (for example one notification status, or one storage provider). */
public record DistributionEntry(String key, long count) {

    public DistributionEntry {
        Objects.requireNonNull(key, "key must not be null");
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
    }
}
