package in.bachatsetu.backend.admin.domain.analytics.port;

import in.bachatsetu.backend.admin.domain.analytics.model.StorageAnalytics;

/** Computes storage analytics on demand — never cached, never scheduled. */
public interface StorageAnalyticsRepository {

    StorageAnalytics compute();
}
