package in.bachatsetu.backend.auth.application.usecase;

import in.bachatsetu.backend.auth.application.command.VerifyOtpCommand;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;

@FunctionalInterface
public interface VerifyOtpUseCase {

    OtpActionResult verify(VerifyOtpCommand command);
}
