package in.bachatsetu.backend.infrastructure.persistence.integration;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceConflictException;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
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
@Import(SavingsGroupPersistencePostgreSqlIntegrationTest.GroupPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class SavingsGroupPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f1000000-0000-0000-0000-000000000001");

    @Autowired
    private SavingsGroupRepository repository;

    @Autowired
    private UserSpringDataRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void persistsRehydratesMembershipHistoryAndSoftDeletes() {
        AggregateId ownerId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        persistUser(ownerId, group.tenantId(), "owner@example.in");
        persistUser(memberId, group.tenantId(), "member@example.in");

        repository.save(group);
        flushAndClear();

        SavingsGroup restored = repository.findById(group.tenantId(), group.groupId()).orElseThrow();
        assertThat(restored.domainEvents()).isEmpty();
        assertThat(restored.description()).isEqualTo(group.description());
        assertThat(restored.members()).hasSize(1);

        restored.activate(ownerId, NOW.plusSeconds(1));
        repository.save(restored);
        flushAndClear();

        SavingsGroup active = repository.findById(group.tenantId(), group.groupId()).orElseThrow();
        active.joinMember(memberId, ownerId, NOW.plusSeconds(2));
        repository.save(active);
        flushAndClear();

        SavingsGroup withMember = repository.findByGroupCode(group.tenantId(), group.code()).orElseThrow();
        assertThat(withMember.memberCount().value()).isEqualTo(2);
        assertThat(repository.existsByGroupCode(group.tenantId(), group.code())).isTrue();
        assertThat(repository.findAll(group.tenantId())).extracting(SavingsGroup::groupId)
                .contains(group.groupId());
        assertThat(historyCount(group.id().value())).isEqualTo(2);

        withMember.removeMember(memberId, ownerId, NOW.plusSeconds(3));
        repository.save(withMember);
        flushAndClear();

        SavingsGroup removed = repository.findById(group.tenantId(), group.groupId()).orElseThrow();
        assertThat(removed.memberCount().value()).isOne();
        assertThat(removed.members()).filteredOn(member -> member.memberId().equals(memberId))
                .singleElement()
                .satisfies(member -> assertThat(member.isActive()).isFalse());
        assertThat(historyCount(group.id().value())).isEqualTo(3);

        repository.delete(group.tenantId(), group.groupId());

        assertThat(repository.findById(group.tenantId(), group.groupId())).isEmpty();
        assertThat(repository.existsByGroupCode(group.tenantId(), group.code())).isFalse();
    }

    @Test
    @Transactional
    void rejectsAStaleAggregateRevision() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        persistUser(ownerId, group.tenantId(), "optimistic@example.in");
        repository.save(group);
        flushAndClear();

        SavingsGroup first = repository.findById(group.tenantId(), group.groupId()).orElseThrow();
        SavingsGroup stale = repository.findById(group.tenantId(), group.groupId()).orElseThrow();
        first.activate(ownerId, NOW.plusSeconds(1));
        stale.close(ownerId, NOW.plusSeconds(1));

        repository.save(first);
        flushAndClear();

        assertThatThrownBy(() -> repository.save(stale))
                .isInstanceOf(PersistenceConflictException.class)
                .hasMessageContaining("stale");
    }

    private void persistUser(AggregateId userId, AggregateId tenantId, String email) {
        userRepository.saveAndFlush(new UserJpaEntity(
                userId.value(),
                tenantId.value(),
                "Savings",
                "Member",
                email,
                null,
                in.bachatsetu.backend.user.domain.model.UserStatus.ACTIVE,
                PreferredLanguage.ENGLISH));
    }

    private int historyCount(UUID groupId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM community.membership_history WHERE group_id = ?",
                Integer.class,
                groupId);
        return count == null ? 0 : count;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class GroupPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
