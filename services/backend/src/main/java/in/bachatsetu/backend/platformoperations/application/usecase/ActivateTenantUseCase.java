package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.command.ActivateTenantCommand;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;

@FunctionalInterface
public interface ActivateTenantUseCase {

    TenantResult execute(ActivateTenantCommand command);
}
