package in.bachatsetu.backend.automation.application.usecase;

import in.bachatsetu.backend.automation.application.query.JobRunResult;

/** Removes orphaned temporary receipt PDF cache files, if any such cache exists. */
@FunctionalInterface
public interface RunReceiptCleanupUseCase {

    JobRunResult execute();
}
