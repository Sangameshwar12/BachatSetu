package in.bachatsetu.backend.automation.application.service;

import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunReceiptCleanupUseCase;
import in.bachatsetu.backend.receipt.application.port.ReceiptPdfGenerator;

/**
 * Deliberate no-op. {@link ReceiptPdfGenerator#generate} renders a receipt PDF entirely in memory
 * (returning a {@code byte[]} straight to its caller) and never writes to disk — there is no temporary PDF
 * cache directory, orphan file, or any other on-disk artifact anywhere in the current Receipt
 * implementation for this job to clean up. This class, and the scheduled job that calls it, exist so the
 * "Receipt Cleanup Job" this sprint requires is present, observable, and testable, without inventing a
 * cache (and the cleanup logic for it) that no other part of the system produces. If a future sprint adds a
 * real on-disk or object-storage PDF cache, this is the place that cache's cleanup would be implemented —
 * behind the same {@link RunReceiptCleanupUseCase} boundary, so the scheduler and its cron trigger require
 * no change.
 */
public final class ReceiptCleanupApplicationService implements RunReceiptCleanupUseCase {

    @Override
    public JobRunResult execute() {
        return JobRunResult.empty();
    }
}
