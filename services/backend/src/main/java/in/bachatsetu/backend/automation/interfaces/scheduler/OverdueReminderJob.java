package in.bachatsetu.backend.automation.interfaces.scheduler;

import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunOverdueReminderUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Triggers {@link RunOverdueReminderUseCase} on a cron schedule. Orchestration only; see {@link DrawSchedulerJob}. */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.automation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OverdueReminderJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverdueReminderJob.class);

    private final RunOverdueReminderUseCase runOverdueReminder;

    public OverdueReminderJob(RunOverdueReminderUseCase runOverdueReminder) {
        this.runOverdueReminder = Objects.requireNonNull(runOverdueReminder, "run overdue reminder must not be null");
    }

    @Scheduled(cron = "${bachatsetu.automation.overdue-reminder.cron}")
    public void run() {
        try {
            JobRunResult result = runOverdueReminder.execute();
            result.failureMessages().forEach(LOGGER::error);
            LOGGER.info(
                    "Overdue reminder run complete: processed={}, skipped={}, failed={}",
                    result.processedCount(), result.skippedCount(), result.failedCount());
        } catch (RuntimeException exception) {
            LOGGER.error("Overdue reminder run failed unexpectedly", exception);
        }
    }
}
