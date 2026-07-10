package in.bachatsetu.backend.admin.domain.analytics.port;

import in.bachatsetu.backend.admin.domain.analytics.model.PaymentAnalytics;

/** Computes payment analytics on demand — never cached, never scheduled. */
public interface PaymentAnalyticsRepository {

    PaymentAnalytics compute();
}
