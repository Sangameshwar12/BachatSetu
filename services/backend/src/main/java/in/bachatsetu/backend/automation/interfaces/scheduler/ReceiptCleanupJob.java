package in.bachatsetu.backend.automation.interfaces.scheduler;

import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunReceiptCleanupUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers {@link RunReceiptCleanupUseCase} on a cron schedule. The use case itself is a deliberate no-op
 * (see its Javadoc) since no receipt PDF cache exists to clean up; this job exists so the schedule and
 * wiring are present and testable ahead of a future cache being introduced.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.automation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ReceiptCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptCleanupJob.class);

    private final RunReceiptCleanupUseCase runReceiptCleanup;

    public ReceiptCleanupJob(RunReceiptCleanupUseCase runReceiptCleanup) {
        this.runReceiptCleanup = Objects.requireNonNull(runReceiptCleanup, "run receipt cleanup must not be null");
    }

    @Scheduled(cron = "${bachatsetu.automation.cleanup.cron}")
    public void run() {
        try {
            JobRunResult result = runReceiptCleanup.execute();
            LOGGER.debug("Receipt cleanup run complete: processed={}", result.processedCount());
        } catch (RuntimeException exception) {
            LOGGER.error("Receipt cleanup run failed unexpectedly", exception);
        }
    }
}
