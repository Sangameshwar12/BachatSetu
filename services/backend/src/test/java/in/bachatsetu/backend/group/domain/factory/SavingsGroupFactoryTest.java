package in.bachatsetu.backend.group.domain.factory;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.monthlyRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.group.domain.model.CreatedAt;
import in.bachatsetu.backend.group.domain.model.GroupDescription;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.service.GroupCodeGenerator;
import in.bachatsetu.backend.group.domain.service.GroupValidationService;
import in.bachatsetu.backend.shared.domain.AggregateId;
import org.junit.jupiter.api.Test;

class SavingsGroupFactoryTest {

    @Test
    void createsCompleteGroupWithGeneratedIdentityAndCode() {
        SavingsGroupFactory factory = new SavingsGroupFactory(
                new GroupCodeGenerator(), new GroupValidationService());
        AggregateId owner = AggregateId.newId();

        SavingsGroup group = factory.create(
                AggregateId.newId(),
                new OwnerId(owner),
                new GroupName("Factory Group"),
                new GroupDescription("Created through domain factory"),
                GroupType.BHISHI,
                monthlyRule(25),
                new CreatedAt(NOW));

        assertThat(group.id()).isNotNull();
        assertThat(group.code().value()).startsWith("BS-").hasSize(19);
        assertThat(group.ownerId()).isEqualTo(new OwnerId(owner));
        assertThat(group.status()).isEqualTo(GroupStatus.INACTIVE);
        assertThat(group.memberCount().value()).isEqualTo(1);
    }

    @Test
    void validatesFactoryDependenciesAndRule() {
        assertThatThrownBy(() -> new SavingsGroupFactory(null, new GroupValidationService()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SavingsGroupFactory(new GroupCodeGenerator(), null))
                .isInstanceOf(NullPointerException.class);

        SavingsGroupFactory factory = new SavingsGroupFactory(
                new GroupCodeGenerator(), new GroupValidationService());
        assertThatThrownBy(() -> factory.create(
                        AggregateId.newId(),
                        new OwnerId(AggregateId.newId()),
                        new GroupName("Missing Rule"),
                        GroupDescription.empty(),
                        GroupType.BHISHI,
                        null,
                        new CreatedAt(NOW)))
                .isInstanceOf(NullPointerException.class);
    }
}
