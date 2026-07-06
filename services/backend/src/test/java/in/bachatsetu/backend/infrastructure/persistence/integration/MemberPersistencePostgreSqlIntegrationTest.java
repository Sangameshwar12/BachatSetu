package in.bachatsetu.backend.infrastructure.persistence.integration;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserStatus;
import jakarta.persistence.EntityManager;
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
@Import(MemberPersistencePostgreSqlIntegrationTest.MemberPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class MemberPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    @Autowired
    private SavingsGroupRepository groupRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserSpringDataRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndRehydratesAMemberProfileWithItsFirstParticipation() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner@example.in");
        AggregateId memberUserId = AggregateId.newId();
        persistUser(memberUserId, group.tenantId(), "member@example.in");
        groupRepository.save(group);
        flushAndClear();

        AggregateId memberId = AggregateId.newId();
        MemberProfile member = MemberProfile.create(
                memberId, group.tenantId(), memberUserId,
                new MemberNumber("MB-1A2B3C4D5E6F7A8B"), group.organizerId(), NOW);
        member.joinGroup(group.id(), GroupRole.MEMBER, group.organizerId(), NOW.plusSeconds(1));

        memberRepository.save(member);
        flushAndClear();

        MemberProfile restored = memberRepository.findById(group.tenantId(), memberId).orElseThrow();
        assertThat(restored.userId()).isEqualTo(memberUserId);
        assertThat(restored.memberNumber().value()).isEqualTo("MB-1A2B3C4D5E6F7A8B");
        assertThat(restored.participations()).singleElement()
                .satisfies(participation -> assertThat(participation.groupId()).isEqualTo(group.id()));

        Optional<MemberProfile> byUser = memberRepository.findByUserId(group.tenantId(), memberUserId);
        assertThat(byUser).isPresent();
        assertThat(memberRepository.findByMemberNumber(group.tenantId(), new MemberNumber("MB-1A2B3C4D5E6F7A8B")))
                .isPresent();
    }

    @Test
    @Transactional
    void reportsNoMatchForAnotherTenant() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner-two@example.in");
        AggregateId memberUserId = AggregateId.newId();
        persistUser(memberUserId, group.tenantId(), "member-two@example.in");
        groupRepository.save(group);
        flushAndClear();

        AggregateId memberId = AggregateId.newId();
        MemberProfile member = MemberProfile.create(
                memberId, group.tenantId(), memberUserId,
                new MemberNumber("MB-2A2B3C4D5E6F7A8B"), group.organizerId(), NOW);
        member.joinGroup(group.id(), GroupRole.MEMBER, group.organizerId(), NOW.plusSeconds(1));
        memberRepository.save(member);
        flushAndClear();

        assertThat(memberRepository.findById(AggregateId.newId(), memberId)).isEmpty();
    }

    private void persistUser(AggregateId userId, AggregateId tenantId, String email) {
        userRepository.saveAndFlush(new UserJpaEntity(
                userId.value(),
                tenantId.value(),
                "Community",
                "Member",
                email,
                null,
                UserStatus.ACTIVE,
                PreferredLanguage.ENGLISH));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MemberPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
