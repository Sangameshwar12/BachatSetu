package in.bachatsetu.backend.auth.application.service;

import in.bachatsetu.backend.auth.application.command.InvalidateOtpCommand;
import in.bachatsetu.backend.auth.application.exception.OtpApplicationException;
import in.bachatsetu.backend.auth.application.exception.OtpFailureReason;
import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.usecase.InvalidateOtpUseCase;
import in.bachatsetu.backend.auth.application.validation.OtpRequestValidator;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.port.OtpVerificationRepository;
import java.util.List;
import java.util.Objects;

public final class InvalidateOtpApplicationService implements InvalidateOtpUseCase {

    private final OtpRequestValidator validator;
    private final OtpVerificationRepository repository;
    private final ClockPort clock;

    public InvalidateOtpApplicationService(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            ClockPort clock) {
        this.validator = Objects.requireNonNull(validator, "OTP validator must not be null");
        this.repository = Objects.requireNonNull(repository, "OTP repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public OtpActionResult invalidate(InvalidateOtpCommand command) {
        Objects.requireNonNull(command, "invalidate OTP command must not be null");
        validator.requireUser(command.userId());
        OtpVerification verification = repository.findActive(command.userId(), command.purpose())
                .orElseThrow(() -> new OtpApplicationException(
                        OtpFailureReason.OTP_NOT_FOUND,
                        "no active OTP exists for the user and purpose"));
        verification.invalidate(command.actorId(), clock.now());
        repository.save(verification);
        return OtpActionResult.from(verification, List.of());
    }
}
