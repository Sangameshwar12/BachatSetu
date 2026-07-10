package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.command.ArchiveTenantCommand;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;

@FunctionalInterface
public interface ArchiveTenantUseCase {

    TenantResult execute(ArchiveTenantCommand command);
}
