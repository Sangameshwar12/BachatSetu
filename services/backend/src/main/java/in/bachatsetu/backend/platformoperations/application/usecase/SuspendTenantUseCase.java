package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.command.SuspendTenantCommand;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;

@FunctionalInterface
public interface SuspendTenantUseCase {

    TenantResult execute(SuspendTenantCommand command);
}
