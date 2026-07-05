package in.bachatsetu.backend.auth.application.usecase;

import in.bachatsetu.backend.auth.application.command.InvalidateOtpCommand;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;

@FunctionalInterface
public interface InvalidateOtpUseCase {

    OtpActionResult invalidate(InvalidateOtpCommand command);
}
