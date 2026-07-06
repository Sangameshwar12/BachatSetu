package in.bachatsetu.backend.member.application;

import in.bachatsetu.backend.member.application.command.CreateMemberProfileCommand;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.function.Supplier;

public final class ApplicationTestFixture {

    private ApplicationTestFixture() {
    }

    public static CreateMemberProfileCommand createCommand() {
        return new CreateMemberProfileCommand(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), GroupRole.MEMBER, AggregateId.newId());
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
