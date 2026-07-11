package in.bachatsetu.backend.auth.application.service;

import in.bachatsetu.backend.auth.application.command.GenerateOtpCommand;
import in.bachatsetu.backend.auth.application.event.OtpApplicationEvent;
import in.bachatsetu.backend.auth.application.event.OtpExpired;
import in.bachatsetu.backend.auth.application.event.OtpRequested;
import in.bachatsetu.backend.auth.application.event.OtpSent;
import in.bachatsetu.backend.auth.application.exception.OtpApplicationException;
import in.bachatsetu.backend.auth.application.exception.OtpFailureReason;
import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.application.port.OtpEventPublisherPort;
import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.auth.application.query.OtpActionResult;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.application.validation.OtpRequestValidator;
import in.bachatsetu.backend.auth.domain.model.OtpCode;
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

public final class GenerateOtpApplicationService implements GenerateOtpUseCase {

    private final OtpRequestValidator validator;
    private final OtpVerificationRepository repository;
    private final OtpPolicyService policyService;
    private final ClockPort clock;
    private final RandomGeneratorPort randomGenerator;
    private final HashingPort hashing;
    private final OtpSenderPort sender;
    private final OtpEventPublisherPort eventPublisher;

    public GenerateOtpApplicationService(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            OtpPolicyService policyService,
            ClockPort clock,
            RandomGeneratorPort randomGenerator,
            HashingPort hashing,
            OtpSenderPort sender,
            OtpEventPublisherPort eventPublisher) {
        this.validator = Objects.requireNonNull(validator, "OTP validator must not be null");
        this.repository = Objects.requireNonNull(repository, "OTP repository must not be null");
        this.policyService = Objects.requireNonNull(policyService, "OTP policy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.randomGenerator = Objects.requireNonNull(randomGenerator, "random generator must not be null");
        this.hashing = Objects.requireNonNull(hashing, "hashing port must not be null");
        this.sender = Objects.requireNonNull(sender, "OTP sender must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
    }

    @Override
    public OtpActionResult generate(GenerateOtpCommand command) {
        Objects.requireNonNull(command, "generate OTP command must not be null");
        User user = validator.requireUser(command.userId());
        Instant now = clock.now();
        List<OtpApplicationEvent> events = new ArrayList<>();
        repository.findActive(command.userId(), command.purpose()).ifPresent(active -> {
            if (!active.expire(command.actorId(), now)) {
                throw new OtpApplicationException(
                        OtpFailureReason.ACTIVE_OTP_EXISTS,
                        "an active OTP already exists for the user and purpose");
            }
            repository.save(active);
            events.add(new OtpExpired(
                    UUID.randomUUID(), active.id(), active.userId(), active.purpose(), now));
        });
        OtpCode code = randomGenerator.generateOtp();
        OtpVerification verification = policyService.generate(
                AggregateId.newId(),
                command.userId(),
                hashing.hash(code),
                command.purpose(),
                command.actorId(),
                now);
        repository.save(verification);
        events.add(new OtpRequested(
                UUID.randomUUID(), verification.id(), verification.userId(), verification.purpose(), now));
        sender.send(user.userId(), user.mobileNumber(), command.purpose(), code);
        events.add(new OtpSent(
                UUID.randomUUID(), verification.id(), verification.userId(), verification.purpose(), now));
        events.forEach(eventPublisher::publish);
        return OtpActionResult.from(verification, events);
    }
}
