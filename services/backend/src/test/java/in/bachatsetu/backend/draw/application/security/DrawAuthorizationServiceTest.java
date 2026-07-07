package in.bachatsetu.backend.draw.application.security;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.draw.application.exception.DrawAccessDeniedException;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import org.junit.jupiter.api.Test;

class DrawAuthorizationServiceTest {

    private final DrawAuthorizationService authorization = new DrawAuthorizationService();

    @Test
    void allowsTheGroupOwner() {
        SavingsGroup group = newGroup(5);

        authorization.requireOwner(group, group.organizerId());
    }

    @Test
    void deniesAnyNonOwnerActor() {
        SavingsGroup group = newGroup(5);
        AggregateId nonOwner = AggregateId.newId();

        assertThatThrownBy(() -> authorization.requireOwner(group, nonOwner))
                .isInstanceOf(DrawAccessDeniedException.class)
                .hasMessage("only the group owner may perform this operation");
    }

    @Test
    void rejectsNullInputs() {
        SavingsGroup group = newGroup(5);

        assertThatThrownBy(() -> authorization.requireOwner(null, group.organizerId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> authorization.requireOwner(group, null))
                .isInstanceOf(NullPointerException.class);
    }
}
