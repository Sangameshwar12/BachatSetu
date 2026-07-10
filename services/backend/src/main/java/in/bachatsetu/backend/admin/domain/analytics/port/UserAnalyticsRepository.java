package in.bachatsetu.backend.admin.domain.analytics.port;

import in.bachatsetu.backend.admin.domain.analytics.model.UserAnalytics;

/** Computes platform user analytics on demand — never cached, never scheduled. */
public interface UserAnalyticsRepository {

    UserAnalytics compute();
}
