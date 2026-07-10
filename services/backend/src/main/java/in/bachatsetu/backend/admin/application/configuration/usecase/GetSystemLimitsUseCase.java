package in.bachatsetu.backend.admin.application.configuration.usecase;

import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import java.util.List;

@FunctionalInterface
public interface GetSystemLimitsUseCase {

    List<PlatformLimitResult> execute();
}
