package in.bachatsetu.backend.auth.domain.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.Permission;
import in.bachatsetu.backend.auth.domain.model.Role;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenHash;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.TokenStatus;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityFactoryTest {

    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void createsIdentityAggregatesWithGeneratedIdsAndFixedTime() {
        AggregateId actorId = AggregateId.newId();
        User user = new UserFactory(CLOCK).register(
                new Email("member@example.com"),
                MobileNumber.of("+919876543210"),
                PasswordHash.encoded("$2b$12$" + "A".repeat(53)),
                actorId);
        Role role = new RoleFactory(CLOCK).create("MEMBER", actorId);
        Permission permission = new PermissionFactory(CLOCK)
                .createUnique("group:read", List.of(), actorId);
        var token = new RefreshTokenFactory(CLOCK, Duration.ofDays(30))
                .issue(
                        user.userId(),
                        AggregateId.newId(),
                        TokenSessionId.newId(),
                        RefreshTokenHash.encoded("H".repeat(60)),
                        actorId);
        var otp = new OtpVerificationFactory(CLOCK)
                .generate(user.userId(), OtpHash.encoded("C".repeat(64)), OtpPurpose.REGISTRATION, actorId);

        assertThat(user.auditInfo().createdAt()).isEqualTo(NOW);
        assertThat(role.auditInfo().createdAt()).isEqualTo(NOW);
        assertThat(permission.auditInfo().createdAt()).isEqualTo(NOW);
        assertThat(token.status()).isEqualTo(TokenStatus.ACTIVE);
        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(otp.status()).isEqualTo(OtpStatus.PENDING);
        assertThat(otp.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void permissionFactoryRejectsCanonicalDuplicateNames() {
        AggregateId actorId = AggregateId.newId();
        PermissionFactory factory = new PermissionFactory(CLOCK);
        Permission existing = factory.createUnique("group:read", List.of(), actorId);

        assertThatThrownBy(() -> factory.createUnique(
                        " GROUP:READ ", Arrays.asList(null, existing), actorId))
                .isInstanceOf(IdentityDomainException.class);
    }

    @Test
    void rejectsNonPositiveFactoryLifetimes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RefreshTokenFactory(CLOCK, Duration.ZERO));
    }
}
