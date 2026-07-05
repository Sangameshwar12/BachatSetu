package in.bachatsetu.backend.auth.application.usecase;

import in.bachatsetu.backend.auth.application.command.ResendOtpCommand;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;

@FunctionalInterface
public interface ResendOtpUseCase {

    OtpActionResult resend(ResendOtpCommand command);
}
