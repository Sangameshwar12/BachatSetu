package in.bachatsetu.backend.automation.application.usecase;

import in.bachatsetu.backend.automation.application.query.JobRunResult;

/** Conducts every scheduled draw whose scheduled time has arrived. */
@FunctionalInterface
public interface RunDrawSchedulerUseCase {

    JobRunResult execute();
}
