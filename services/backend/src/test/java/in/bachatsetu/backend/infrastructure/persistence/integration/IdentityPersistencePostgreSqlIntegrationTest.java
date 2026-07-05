package in.bachatsetu.backend.infrastructure.persistence.integration;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.OtpHash;
import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.model.PasswordHash;
import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.Role;
import in.bachatsetu.backend.auth.domain.model.User;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.auth.domain.port.OtpVerificationRepository;
import in.bachatsetu.backend.auth.domain.port.PermissionRepository;
import in.bachatsetu.backend.auth.domain.port.RefreshTokenRepository;
import in.bachatsetu.backend.auth.domain.port.RoleRepository;
import in.bachatsetu.backend.auth.domain.service.OtpPolicyService;
import in.bachatsetu.backend.infrastructure.persistence.adapter.TenantScopeProvider;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        classes = BachatSetuBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.flyway.enabled=true",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.data.redis.repositories.enabled=false",
            "bachatsetu.persistence.auditing.enabled=true",
            "bachatsetu.persistence.repositories.enabled=true"
        })
@Import(IdentityPersistencePostgreSqlIntegrationTest.IdentityTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class IdentityPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("f0000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("f0000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-07-05T06:00:00Z");
    private static final String PASSWORD_HASH = "$2b$12$" + "A".repeat(53);

    @Autowired
    private UserSpringDataRepository userJpaRepository;

    @Autowired
    private in.bachatsetu.backend.auth.domain.port.UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @Test
    @Transactional
    void persistsIdentityAggregatesThroughDomainPorts() {
        UserId userId = new UserId(UUID.randomUUID());
        UserJpaEntity canonicalUser = new UserJpaEntity(
                userId.value(),
                TENANT_ID,
                "Asha",
                "Patil",
                "asha@example.in",
                "+919876543210",
                in.bachatsetu.backend.user.domain.model.UserStatus.ACTIVE,
                PreferredLanguage.MARATHI);
        userJpaRepository.saveAndFlush(canonicalUser);

        Role role = roleRepository.findByName("GROUP_MEMBER").orElseThrow();
        assertThat(permissionRepository.findByName("GROUP.READ")).isPresent();
        User user = User.register(
                userId,
                new Email("asha@example.in"),
                MobileNumber.of("+919876543210"),
                PasswordHash.encoded(PASSWORD_HASH),
                new AggregateId(ACTOR_ID),
                NOW);
        user.assignRole(role.roleId(), new AggregateId(ACTOR_ID), NOW.plusSeconds(1));
        userRepository.save(user);

        User restoredUser = userRepository.findByEmail(new Email("asha@example.in")).orElseThrow();
        assertThat(restoredUser.roleIds()).containsExactly(role.roleId());
        assertThat(restoredUser.domainEvents()).isEmpty();

        RefreshToken token = RefreshToken.issue(
                RefreshTokenId.newId(),
                userId,
                NOW,
                NOW.plusSeconds(3600),
                new AggregateId(ACTOR_ID));
        refreshTokenRepository.save(token);
        assertThat(refreshTokenRepository.findById(token.refreshTokenId())).isPresent();

        OtpVerification otp = OtpVerification.generate(
                AggregateId.newId(),
                userId,
                OtpHash.encoded("C".repeat(64)),
                OtpPurpose.SIGN_IN,
                NOW,
                NOW.plusSeconds(300),
                new AggregateId(ACTOR_ID));
        otpVerificationRepository.save(otp);
        assertThat(otpVerificationRepository.findById(otp.id())).isPresent();

        OtpVerification replacement = new OtpPolicyService().resend(
                otp,
                AggregateId.newId(),
                OtpHash.encoded("D".repeat(64)),
                new AggregateId(ACTOR_ID),
                NOW.plusSeconds(30));
        otpVerificationRepository.replace(otp, replacement);
        assertThat(otpVerificationRepository.findActive(userId, OtpPurpose.SIGN_IN))
                .get()
                .extracting(OtpVerification::id)
                .isEqualTo(replacement.id());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class IdentityTestConfiguration {

        @Bean
        TenantScopeProvider tenantScopeProvider() {
            return () -> new AggregateId(TENANT_ID);
        }

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(ACTOR_ID);
        }
    }
}
