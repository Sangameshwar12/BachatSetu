package in.bachatsetu.backend.member.application;

import in.bachatsetu.backend.member.application.command.CreateMemberProfileCommand;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.model.MemberStatus;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.function.Supplier;

public final class ApplicationTestFixture {

    public static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    private ApplicationTestFixture() {
    }

    public static CreateMemberProfileCommand createCommand() {
        AggregateId userId = AggregateId.newId();
        return new CreateMemberProfileCommand(
                AggregateId.newId(), userId, AggregateId.newId(), GroupRole.MEMBER, userId);
    }

    public static MemberProfile activeMember(AggregateId tenantId) {
        MemberProfile member = MemberProfile.create(
                AggregateId.newId(), tenantId, AggregateId.newId(),
                new MemberNumber("MB-FIXTURE000001"), AggregateId.newId(), NOW);
        member.joinGroup(AggregateId.newId(), GroupRole.MEMBER, member.userId(), NOW.plusSeconds(1));
        member.changeStatus(MemberStatus.ACTIVE, member.userId(), NOW.plusSeconds(2));
        member.pullDomainEvents();
        return member;
    }

    public static TransactionPort directTransaction() {
        return new TransactionPort() {
            @Override
            public <T> T execute(Supplier<T> operation) {
                return operation.get();
            }
        };
    }
}
