package in.bachatsetu.backend.auth.application.service;

import in.bachatsetu.backend.auth.application.command.ResendOtpCommand;
import in.bachatsetu.backend.auth.application.event.OtpApplicationEvent;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpResent;
import in.bachatsetu.backend.auth.application.event.OtpSent;
import in.bachatsetu.backend.auth.application.exception.OtpApplicationException;
import in.bachatsetu.backend.auth.application.exception.OtpFailureReason;
import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.usecase.ResendOtpUseCase;
import in.bachatsetu.backend.auth.application.validation.OtpRequestValidator;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.port.OtpVerificationRepository;
import in.bachatsetu.backend.auth.domain.service.OtpPolicyService;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ResendOtpApplicationService implements ResendOtpUseCase {

    private final OtpRequestValidator validator;
    private final OtpVerificationRepository repository;
    private final OtpPolicyService policyService;
    private final ClockPort clock;
    private final RandomGeneratorPort randomGenerator;
    private final HashingPort hashing;
    private final OtpSenderPort sender;

    public ResendOtpApplicationService(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            OtpPolicyService policyService,
            ClockPort clock,
            RandomGeneratorPort randomGenerator,
            HashingPort hashing,
            OtpSenderPort sender) {
        this.validator = Objects.requireNonNull(validator, "OTP validator must not be null");
        this.repository = Objects.requireNonNull(repository, "OTP repository must not be null");
        this.policyService = Objects.requireNonNull(policyService, "OTP policy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.randomGenerator = Objects.requireNonNull(randomGenerator, "random generator must not be null");
        this.hashing = Objects.requireNonNull(hashing, "hashing port must not be null");
        this.sender = Objects.requireNonNull(sender, "OTP sender must not be null");
    }

    @Override
    public OtpActionResult resend(ResendOtpCommand command) {
        Objects.requireNonNull(command, "resend OTP command must not be null");
        User user = validator.requireUser(command.userId());
        OtpVerification current = repository.findActive(command.userId(), command.purpose())
                .orElseThrow(() -> new OtpApplicationException(
                        OtpFailureReason.OTP_NOT_FOUND,
                        "no OTP exists to resend for the user and purpose"));
        if (current.resendCount() >= OtpVerification.MAXIMUM_RESENDS) {
            throw new OtpApplicationException(
                    OtpFailureReason.RESEND_LIMIT_REACHED, "maximum OTP resend count reached");
        }
        Instant now = clock.now();
        OtpCode code = randomGenerator.generateOtp();
        OtpVerification replacement = policyService.resend(
                current,
                AggregateId.newId(),
                hashing.hash(code),
                command.actorId(),
                now);
        repository.replace(current, replacement);
        sender.send(user.userId(), user.mobileNumber(), command.purpose(), code);
        List<OtpApplicationEvent> events = new ArrayList<>();
        if (current.status() == OtpStatus.EXPIRED) {
            events.add(new OtpExpired(
                    UUID.randomUUID(), current.id(), current.userId(), current.purpose(), now));
        }
        events.add(new OtpResent(
                UUID.randomUUID(),
                replacement.id(),
                current.id(),
                replacement.userId(),
                replacement.purpose(),
                now));
        events.add(new OtpSent(
                UUID.randomUUID(), replacement.id(), replacement.userId(), replacement.purpose(), now));
        return OtpActionResult.from(replacement, events);
    }
}
