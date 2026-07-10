package in.bachatsetu.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.domain.event.PasswordChanged;
import in.bachatsetu.backend.auth.domain.event.UserActivated;
import in.bachatsetu.backend.auth.domain.event.UserRegistered;
import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");
    private static final String FIRST_HASH = "$2b$12$" + "A".repeat(53);
    private static final String SECOND_HASH = "$2b$12$" + "B".repeat(53);

    @Test
    void registersAssignsOneRoleAndChangesPassword() {
        UserId userId = UserId.newId();
        AggregateId actorId = AggregateId.newId();
        PasswordHash firstHash = PasswordHash.encoded(FIRST_HASH);
        User user = User.register(
                userId,
                new Email("member@example.com"),
                MobileNumber.of("+919876543210"),
                firstHash,
                actorId,
                NOW);

        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.email()).isEqualTo(new Email("member@example.com"));
        assertThat(user.mobileNumber()).isEqualTo(MobileNumber.of("+919876543210"));
        assertThat(user.passwordHash()).isEqualTo(firstHash);
        assertThat(user.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(user.roleIds()).isEmpty();
        assertThat(user.domainEvents()).singleElement().isInstanceOf(UserRegistered.class);
        UserRegistered registered = (UserRegistered) user.domainEvents().getFirst();
        assertThat(registered.aggregateId()).isEqualTo(userId.toAggregateId());
        assertThat(registered.eventType()).isEqualTo("UserRegistered");

        RoleId roleId = RoleId.newId();
        user.assignRole(roleId, actorId, NOW.plusSeconds(1));
        assertThat(user.roleIds()).containsExactly(roleId);
        assertThatThrownBy(() -> user.roleIds().add(RoleId.newId()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> user.assignRole(roleId, actorId, NOW.plusSeconds(2)))
                .isInstanceOf(IdentityDomainException.class);

        PasswordHash secondHash = PasswordHash.encoded(SECOND_HASH);
        user.changePassword(secondHash, actorId, NOW.plusSeconds(2));
        assertThat(user.passwordHash()).isEqualTo(secondHash);
        assertThat(user.domainEvents()).anyMatch(PasswordChanged.class::isInstance);
        PasswordChanged changed = (PasswordChanged) user.domainEvents().getLast();
        assertThat(changed.aggregateId()).isEqualTo(userId.toAggregateId());
        assertThatThrownBy(() -> user.changePassword(secondHash, actorId, NOW.plusSeconds(3)))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void activatesAPendingVerificationUserExactlyOnce() {
        UserId userId = UserId.newId();
        AggregateId actorId = AggregateId.newId();
        User user = newUser(userId, actorId);

        user.activate(actorId, NOW.plusSeconds(1));

        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.domainEvents()).anyMatch(UserActivated.class::isInstance);
        UserActivated activated = (UserActivated) user.domainEvents().getLast();
        assertThat(activated.aggregateId()).isEqualTo(userId.toAggregateId());
        assertThatThrownBy(() -> user.activate(actorId, NOW.plusSeconds(2)))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void equalityUsesTypedUserIdentity() {
        UserId userId = UserId.newId();
        AggregateId actorId = AggregateId.newId();
        User first = newUser(userId, actorId);
        User sameIdentity = newUser(userId, actorId);
        User different = newUser(UserId.newId(), actorId);

        assertThat(first).isEqualTo(first).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
        assertThat(first).isNotEqualTo(different).isNotEqualTo(null).isNotEqualTo("user");
    }

    @Test
    void rehydratesCompleteStateWithoutEvents() {
        UserId userId = UserId.newId();
        RoleId roleId = RoleId.newId();
        AggregateId actorId = AggregateId.newId();
        var auditInfo = in.bachatsetu.backend.shared.domain.AuditInfo.createdBy(actorId, NOW);

        User user = User.rehydrate(
                userId,
                new Email("member@example.com"),
                MobileNumber.of("+919876543210"),
                PasswordHash.encoded(FIRST_HASH),
                UserStatus.ACTIVE,
                Set.of(roleId),
                auditInfo,
                7);

        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.roleIds()).containsExactly(roleId);
        assertThat(user.auditInfo()).isEqualTo(auditInfo);
        assertThat(user.version()).isEqualTo(7);
        assertThat(user.domainEvents()).isEmpty();
    }

    private User newUser(UserId userId, AggregateId actorId) {
        return User.register(
                userId,
                new Email("member@example.com"),
                MobileNumber.of("+919876543210"),
                PasswordHash.encoded(FIRST_HASH),
                actorId,
                NOW);
    }
}
