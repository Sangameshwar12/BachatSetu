package in.bachatsetu.backend.security.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextCurrentAuditorProviderTest {

    private static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final SecurityContextCurrentAuditorProvider provider =
            new SecurityContextCurrentAuditorProvider(SYSTEM_ACTOR_ID);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheRealSignedInUsersIdWhenAuthenticated() {
        AuthenticatedUser user = user();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, AuthorityUtils.NO_AUTHORITIES));

        assertThat(provider.currentAuditorId()).contains(user.userId().value());
    }

    @Test
    void fallsBackToTheSystemActorWhenNoOneIsAuthenticated() {
        assertThat(provider.currentAuditorId()).contains(SYSTEM_ACTOR_ID);

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated(user(), null));
        assertThat(provider.currentAuditorId()).contains(SYSTEM_ACTOR_ID);

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(provider.currentAuditorId()).contains(SYSTEM_ACTOR_ID);
    }

    @Test
    void rejectsANullSystemActorId() {
        assertThatThrownBy(() -> new SecurityContextCurrentAuditorProvider(null))
                .isInstanceOf(NullPointerException.class);
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("MEMBER"),
                Set.of());
    }
}
