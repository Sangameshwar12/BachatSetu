package in.bachatsetu.backend.member.application.service;

import in.bachatsetu.backend.member.application.command.CreateMemberProfileCommand;
import in.bachatsetu.backend.member.application.exception.DuplicateMemberNumberException;
import in.bachatsetu.backend.member.application.mapper.MemberApplicationMapper;
import in.bachatsetu.backend.member.application.port.ClockPort;
import in.bachatsetu.backend.member.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.member.application.port.MemberNumberGeneratorPort;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.usecase.CreateMemberProfileUseCase;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/** Coordinates member profile creation without owning business invariants. */
public final class CreateMemberProfileApplicationService implements CreateMemberProfileUseCase {

    private final MemberRepository repository;
    private final MemberNumberGeneratorPort numberGenerator;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final MemberApplicationSupport support;

    public CreateMemberProfileApplicationService(
            MemberRepository repository,
            MemberNumberGeneratorPort numberGenerator,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            MemberApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.numberGenerator = Objects.requireNonNull(numberGenerator, "number generator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.support = new MemberApplicationSupport(repository, eventPublisher, mapper);
    }

    @Override
    public MemberProfileResult execute(CreateMemberProfileCommand command) {
        Objects.requireNonNull(command, "create command must not be null");
        return transaction.execute(() -> create(command));
    }

    private MemberProfileResult create(CreateMemberProfileCommand command) {
        AggregateId memberId = AggregateId.newId();
        MemberNumber memberNumber = Objects.requireNonNull(
                numberGenerator.generate(memberId), "generated member number must not be null");
        if (repository.findByMemberNumber(command.tenantId(), memberNumber).isPresent()) {
            throw new DuplicateMemberNumberException("generated member number already exists");
        }
        Instant now = clock.now();
        MemberProfile member = MemberProfile.create(
                memberId, command.tenantId(), command.userId(), memberNumber, command.actorId(), now);
        member.joinGroup(command.groupId(), command.role(), command.actorId(), now);
        return support.saveAndPublish(member);
    }
}
