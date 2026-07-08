package in.bachatsetu.backend.automation.application.usecase;

import in.bachatsetu.backend.automation.application.query.JobRunResult;

/** Queues a reminder notification for every unpaid contribution whose due date has already passed. */
@FunctionalInterface
public interface RunOverdueReminderUseCase {

    JobRunResult execute();
}
