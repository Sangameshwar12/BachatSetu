package in.bachatsetu.backend.admin.application.analytics.usecase;

import in.bachatsetu.backend.admin.application.analytics.command.ViewAnalyticsCommand;
import in.bachatsetu.backend.admin.application.analytics.query.PaymentAnalyticsResult;

/** Computes payment analytics. */
@FunctionalInterface
public interface GetPaymentAnalyticsUseCase {

    PaymentAnalyticsResult execute(ViewAnalyticsCommand command);
}
