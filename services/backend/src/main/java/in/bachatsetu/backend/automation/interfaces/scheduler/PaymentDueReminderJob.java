package in.bachatsetu.backend.automation.interfaces.scheduler;

import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunPaymentDueReminderUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Triggers {@link RunPaymentDueReminderUseCase} on a cron schedule. Orchestration only; see {@link DrawSchedulerJob}. */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.automation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentDueReminderJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentDueReminderJob.class);

    private final RunPaymentDueReminderUseCase runPaymentDueReminder;

    public PaymentDueReminderJob(RunPaymentDueReminderUseCase runPaymentDueReminder) {
        this.runPaymentDueReminder =
                Objects.requireNonNull(runPaymentDueReminder, "run payment due reminder must not be null");
    }

    @Scheduled(cron = "${bachatsetu.automation.payment-reminder.cron}")
    public void run() {
        try {
            JobRunResult result = runPaymentDueReminder.execute();
            result.failureMessages().forEach(LOGGER::error);
            LOGGER.info(
                    "Payment due reminder run complete: processed={}, skipped={}, failed={}",
                    result.processedCount(), result.skippedCount(), result.failedCount());
        } catch (RuntimeException exception) {
            LOGGER.error("Payment due reminder run failed unexpectedly", exception);
        }
    }
}
