package in.bachatsetu.backend.auth.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CurrentUserContractTest {

    @Test
    void exposesImmutableAuthenticatedIdentity() {
        AuthenticatedUser user = user();
        CurrentUserProvider provider = () -> Optional.of(user);

        assertThat(provider.requireCurrentUser()).isEqualTo(user);
        assertThat(user.userId()).isNotNull();
        assertThat(user.mobileNumber().value()).isEqualTo("+919876543210");
        assertThat(user.tenantId()).isNotNull();
        assertThat(user.roles()).containsExactly("GROUP_MEMBER");
        assertThat(user.permissions()).containsExactly("group.read");
        assertThatThrownBy(() -> user.roles().add("ADMIN"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsMissingCurrentUser() {
        CurrentUserProvider provider = Optional::empty;

        assertThatThrownBy(provider::requireCurrentUser)
                .isInstanceOf(CurrentUserUnavailableException.class)
                .hasMessage("no authenticated user is available");
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("group.read"));
    }
}
