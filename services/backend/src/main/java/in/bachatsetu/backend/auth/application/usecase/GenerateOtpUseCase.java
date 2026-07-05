package in.bachatsetu.backend.auth.application.usecase;

import in.bachatsetu.backend.auth.application.command.GenerateOtpCommand;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;

@FunctionalInterface
public interface GenerateOtpUseCase {

    OtpActionResult generate(GenerateOtpCommand command);
}
