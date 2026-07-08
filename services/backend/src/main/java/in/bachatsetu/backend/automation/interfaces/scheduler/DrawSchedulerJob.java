package in.bachatsetu.backend.automation.interfaces.scheduler;

import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunDrawSchedulerUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers {@link RunDrawSchedulerUseCase} on a cron schedule. Contains no business logic of its own —
 * every rule (which draws qualify, how one is conducted, per-draw failure handling) lives in the use case
 * and the pre-existing Draw domain/application layers this orchestrates. This method's only additional
 * responsibility beyond delegation is a defensive top-level catch, so a completely unexpected failure
 * (for example, a mis-wired dependency) logs instead of silently killing future scheduled runs.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.automation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DrawSchedulerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DrawSchedulerJob.class);

    private final RunDrawSchedulerUseCase runDrawScheduler;

    public DrawSchedulerJob(RunDrawSchedulerUseCase runDrawScheduler) {
        this.runDrawScheduler = Objects.requireNonNull(runDrawScheduler, "run draw scheduler must not be null");
    }

    @Scheduled(cron = "${bachatsetu.automation.draw.cron}")
    public void run() {
        try {
            JobRunResult result = runDrawScheduler.execute();
            result.failureMessages().forEach(LOGGER::error);
            LOGGER.info(
                    "Draw scheduler run complete: processed={}, failed={}",
                    result.processedCount(), result.failedCount());
        } catch (RuntimeException exception) {
            LOGGER.error("Draw scheduler run failed unexpectedly", exception);
        }
    }
}
