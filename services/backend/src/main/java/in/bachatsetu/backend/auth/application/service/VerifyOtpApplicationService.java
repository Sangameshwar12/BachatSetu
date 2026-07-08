package in.bachatsetu.backend.auth.application.service;

import in.bachatsetu.backend.auth.application.command.VerifyOtpCommand;
import in.bachatsetu.backend.auth.application.event.OtpApplicationEvent;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRejected;
import in.bachatsetu.backend.auth.application.event.OtpRejectionReason;
import in.bachatsetu.backend.auth.application.event.OtpVerified;
import in.bachatsetu.backend.auth.application.exception.OtpApplicationException;
import in.bachatsetu.backend.auth.application.exception.OtpFailureReason;
import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.application.port.OtpEventPublisherPort;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.application.validation.OtpRequestValidator;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.port.OtpVerificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class VerifyOtpApplicationService implements VerifyOtpUseCase {

    private final OtpRequestValidator validator;
    private final OtpVerificationRepository repository;
    private final ClockPort clock;
    private final HashingPort hashing;
    private final OtpEventPublisherPort eventPublisher;

    public VerifyOtpApplicationService(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            ClockPort clock,
            HashingPort hashing,
            OtpEventPublisherPort eventPublisher) {
        this.validator = Objects.requireNonNull(validator, "OTP validator must not be null");
        this.repository = Objects.requireNonNull(repository, "OTP repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.hashing = Objects.requireNonNull(hashing, "hashing port must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
    }

    @Override
    public OtpActionResult verify(VerifyOtpCommand command) {
        Objects.requireNonNull(command, "verify OTP command must not be null");
        validator.requireUser(command.userId());
        OtpVerification verification = repository.findActive(command.userId(), command.purpose())
                .orElseThrow(() -> new OtpApplicationException(
                        OtpFailureReason.OTP_NOT_FOUND,
                        "no active OTP exists for the user and purpose"));
        Instant now = clock.now();
        if (verification.expire(command.actorId(), now)) {
            repository.save(verification);
            return result(verification, new OtpExpired(
                    UUID.randomUUID(), verification.id(), verification.userId(), verification.purpose(), now));
        }
        boolean verified = verification.verify(
                hashing.matches(command.code(), verification.hash()), command.actorId(), now);
        repository.save(verification);
        OtpApplicationEvent event = verified
                ? new OtpVerified(
                        UUID.randomUUID(), verification.id(), verification.userId(), verification.purpose(), now)
                : new OtpRejected(
                        UUID.randomUUID(),
                        verification.id(),
                        verification.userId(),
                        verification.purpose(),
                        verification.status() == OtpStatus.FAILED
                                ? OtpRejectionReason.ATTEMPT_LIMIT
                                : OtpRejectionReason.INVALID_CODE,
                        now);
        return result(verification, event);
    }

    private OtpActionResult result(OtpVerification verification, OtpApplicationEvent event) {
        eventPublisher.publish(event);
        return OtpActionResult.from(verification, List.of(event));
    }
}
