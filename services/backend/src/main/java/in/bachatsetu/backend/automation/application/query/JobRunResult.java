package in.bachatsetu.backend.automation.application.query;

import java.util.List;
import java.util.Objects;

/**
 * Summary of one scheduled job run: how many candidate items were successfully processed, how many were
 * skipped (for example, a reminder already sent today), and a message per item that failed and was logged
 * without stopping the rest of the run. {@code failedCount()} is derived from
 * {@link #failureMessages()}'s size rather than stored separately, so the two can never disagree.
 *
 * <p>Deliberately carries only plain data (counts and {@link String} messages): the application layer that
 * produces this result must not depend on a logging framework (see {@code ArchitecturePackages} /
 * {@code LayerDependencyArchitectureTest}), so it collects failure messages here instead of logging them
 * itself — the {@code interfaces.scheduler} job that calls the use case is what actually logs them.
 */
public record JobRunResult(int processedCount, int skippedCount, List<String> failureMessages) {

    public JobRunResult {
        if (processedCount < 0 || skippedCount < 0) {
            throw new IllegalArgumentException("job run counts must not be negative");
        }
        failureMessages = List.copyOf(Objects.requireNonNull(failureMessages, "failureMessages must not be null"));
    }

    public int failedCount() {
        return failureMessages.size();
    }

    public static JobRunResult empty() {
        return new JobRunResult(0, 0, List.of());
    }
}
