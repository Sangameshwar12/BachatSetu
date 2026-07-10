package in.bachatsetu.backend.admin.domain.analytics.port;

import in.bachatsetu.backend.admin.domain.analytics.model.OverviewAnalytics;

/** Computes the platform-wide overview snapshot on demand — never cached, never scheduled. */
public interface OverviewAnalyticsRepository {

    OverviewAnalytics compute();
}
