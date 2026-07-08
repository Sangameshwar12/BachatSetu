package in.bachatsetu.backend.automation.application.usecase;

import in.bachatsetu.backend.automation.application.query.JobRunResult;

/** Queues a reminder notification for every unpaid contribution nearing its due date. */
@FunctionalInterface
public interface RunPaymentDueReminderUseCase {

    JobRunResult execute();
}
