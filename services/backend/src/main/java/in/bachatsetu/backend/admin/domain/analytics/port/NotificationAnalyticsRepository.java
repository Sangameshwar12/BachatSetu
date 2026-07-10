package in.bachatsetu.backend.admin.domain.analytics.port;

import in.bachatsetu.backend.admin.domain.analytics.model.NotificationAnalytics;

/** Computes notification analytics on demand — never cached, never scheduled. */
public interface NotificationAnalyticsRepository {

    NotificationAnalytics compute();
}
