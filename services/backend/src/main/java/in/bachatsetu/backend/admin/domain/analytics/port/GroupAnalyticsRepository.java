package in.bachatsetu.backend.admin.domain.analytics.port;

import in.bachatsetu.backend.admin.domain.analytics.model.GroupAnalytics;

/** Computes savings group analytics on demand — never cached, never scheduled. */
public interface GroupAnalyticsRepository {

    GroupAnalytics compute();
}
