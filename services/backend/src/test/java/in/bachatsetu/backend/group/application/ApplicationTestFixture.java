package in.bachatsetu.backend.group.application;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.monthlyRule;

import in.bachatsetu.backend.group.application.command.CreateSavingsGroupCommand;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.domain.model.GroupDescription;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.function.Supplier;

public final class ApplicationTestFixture {

    private ApplicationTestFixture() {
    }

    public static CreateSavingsGroupCommand createCommand() {
        return new CreateSavingsGroupCommand(
                AggregateId.newId(),
                new OwnerId(AggregateId.newId()),
                new GroupName("Application Group"),
                new GroupDescription("Application layer test group"),
                GroupType.BHISHI,
                monthlyRule(5));
    }

    public static SavingsGroup activeGroup() {
        SavingsGroup group = in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup(5);
        group.activate(group.organizerId(), NOW.plusSeconds(1));
        group.pullDomainEvents();
        return group;
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
