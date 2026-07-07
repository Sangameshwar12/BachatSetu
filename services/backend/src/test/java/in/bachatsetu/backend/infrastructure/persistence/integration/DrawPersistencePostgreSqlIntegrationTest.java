package in.bachatsetu.backend.infrastructure.persistence.integration;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.draw.domain.model.BidAmount;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.draw.domain.port.DrawSortField;
import in.bachatsetu.backend.draw.domain.port.SortDirection;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.CycleStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MonthlyCycleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MonthlyCycleSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.member.domain.model.GroupParticipation;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
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
@Import(DrawPersistencePostgreSqlIntegrationTest.DrawPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class DrawPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f4000000-0000-0000-0000-000000000001");

    @Autowired
    private SavingsGroupRepository groupRepository;

    @Autowired
    private SavingsGroupSpringDataRepository groupSpringDataRepository;

    @Autowired
    private MonthlyCycleSpringDataRepository cycleRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private DrawRepository drawRepository;

    @Autowired
    private UserSpringDataRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndRehydratesAScheduledDraw() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner@example.in");
        groupRepository.save(group);
        flushAndClear();
        MonthlyCycleJpaEntity cycle = persistCycle(group, 1);

        AggregateId drawId = AggregateId.newId();
        Draw draw = Draw.schedule(
                drawId, group.tenantId(), group.id(), new AggregateId(cycle.getId()),
                new DrawNumber(1), DrawType.RANDOM, NOW.plusSeconds(3600), group.organizerId(), NOW);

        drawRepository.save(draw);
        flushAndClear();

        Draw restored = drawRepository.findById(group.tenantId(), drawId).orElseThrow();
        assertThat(restored.groupId()).isEqualTo(group.id());
        assertThat(restored.cycleId()).isEqualTo(new AggregateId(cycle.getId()));
        assertThat(restored.number().value()).isEqualTo(1);
        assertThat(restored.type()).isEqualTo(DrawType.RANDOM);
        assertThat(restored.status().name()).isEqualTo("SCHEDULED");
        assertThat(restored.winnerMemberId()).isNull();
        assertThat(restored.bids()).isEmpty();
    }

    @Test
    @Transactional
    void reportsNoMatchForAnotherTenant() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner-two@example.in");
        groupRepository.save(group);
        flushAndClear();
        MonthlyCycleJpaEntity cycle = persistCycle(group, 1);

        AggregateId drawId = AggregateId.newId();
        Draw draw = Draw.schedule(
                drawId, group.tenantId(), group.id(), new AggregateId(cycle.getId()),
                new DrawNumber(1), DrawType.RANDOM, NOW.plusSeconds(3600), group.organizerId(), NOW);
        drawRepository.save(draw);
        flushAndClear();

        assertThat(drawRepository.findById(AggregateId.newId(), drawId)).isEmpty();
    }

    @Test
    @Transactional
    void persistsAndRehydratesACompletedAuctionDrawWithWinningBid() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner-three@example.in");
        groupRepository.save(group);
        flushAndClear();
        MonthlyCycleJpaEntity cycle = persistCycle(group, 1);
        AggregateId winnerId = persistMember(group, "MB-WINNER0000001", "winner@example.in");
        flushAndClear();

        AggregateId drawId = AggregateId.newId();
        Draw draw = Draw.schedule(
                drawId, group.tenantId(), group.id(), new AggregateId(cycle.getId()),
                new DrawNumber(1), DrawType.AUCTION, NOW.plusSeconds(3600), group.organizerId(), NOW);
        draw.open(group.organizerId(), NOW.plusSeconds(3600));
        draw.submitBid(winnerId, new BidAmount(Money.inr(25_000)), group.organizerId(), NOW.plusSeconds(3700));
        draw.complete(winnerId, group.organizerId(), NOW.plusSeconds(3800));

        drawRepository.save(draw);
        flushAndClear();

        Draw restored = drawRepository.findById(group.tenantId(), drawId).orElseThrow();
        assertThat(restored.status().name()).isEqualTo("COMPLETED");
        assertThat(restored.winnerMemberId()).isEqualTo(winnerId);
        assertThat(restored.bids()).singleElement().satisfies(bid -> {
            assertThat(bid.memberId()).isEqualTo(winnerId);
            assertThat(bid.status().name()).isEqualTo("ACCEPTED");
        });
    }

    @Test
    @Transactional
    void paginatesAndSortsDrawsAtTheDatabase() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner-four@example.in");
        AggregateId tenantId = group.tenantId();
        groupRepository.save(group);
        flushAndClear();

        Draw earliest = newScheduledDraw(group, 1, NOW.plusSeconds(3600));
        Draw middle = newScheduledDraw(group, 2, NOW.plusSeconds(7200));
        Draw latest = newScheduledDraw(group, 3, NOW.plusSeconds(10_800));
        drawRepository.save(earliest);
        drawRepository.save(middle);
        drawRepository.save(latest);
        flushAndClear();

        DrawPage<Draw> byScheduledAtAscending = drawRepository.findPage(
                tenantId, new DrawPageRequest(0, 2, DrawSortField.SCHEDULED_AT, SortDirection.ASC));
        assertThat(byScheduledAtAscending.content()).extracting(draw -> draw.number().value())
                .containsExactly(1, 2);
        assertThat(byScheduledAtAscending.totalElements()).isEqualTo(3);
        assertThat(byScheduledAtAscending.hasNext()).isTrue();
        assertThat(byScheduledAtAscending.hasPrevious()).isFalse();

        DrawPage<Draw> secondPage = drawRepository.findPage(
                tenantId, new DrawPageRequest(1, 2, DrawSortField.SCHEDULED_AT, SortDirection.ASC));
        assertThat(secondPage.content()).extracting(draw -> draw.number().value()).containsExactly(3);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.hasPrevious()).isTrue();

        DrawPage<Draw> byCreatedAtDescending = drawRepository.findPage(
                tenantId, new DrawPageRequest(0, 10, DrawSortField.CREATED_AT, SortDirection.DESC));
        assertThat(byCreatedAtDescending.content()).extracting(draw -> draw.number().value())
                .containsExactly(3, 2, 1);
    }

    private Draw newScheduledDraw(SavingsGroup group, int cycleNumber, Instant scheduledAt) {
        MonthlyCycleJpaEntity cycle = persistCycle(group, cycleNumber);
        return Draw.schedule(
                AggregateId.newId(), group.tenantId(), group.id(), new AggregateId(cycle.getId()),
                new DrawNumber(cycleNumber), DrawType.RANDOM, scheduledAt, group.organizerId(), NOW);
    }

    private MonthlyCycleJpaEntity persistCycle(SavingsGroup group, int cycleNumber) {
        SavingsGroupJpaEntity groupEntity = groupSpringDataRepository.findByIdAndDeletedFalse(group.id().value())
                .orElseThrow();
        MonthlyCycleJpaEntity cycle = new MonthlyCycleJpaEntity(
                UUID.randomUUID(), group.tenantId().value(), groupEntity, cycleNumber,
                LocalDate.of(2026, 7, cycleNumber), LocalDate.of(2026, 8, cycleNumber),
                CycleStatus.OPEN, null, null);
        return cycleRepository.saveAndFlush(cycle);
    }

    private AggregateId persistMember(SavingsGroup group, String memberNumber, String email) {
        AggregateId userId = AggregateId.newId();
        persistUser(userId, group.tenantId(), email);
        MemberProfile member = MemberProfile.create(
                AggregateId.newId(), group.tenantId(), userId, new MemberNumber(memberNumber),
                group.organizerId(), NOW);
        GroupParticipation participation =
                member.joinGroup(group.id(), GroupRole.MEMBER, group.organizerId(), NOW);
        memberRepository.save(member);
        return participation.id();
    }

    private void persistUser(AggregateId userId, AggregateId tenantId, String email) {
        userRepository.saveAndFlush(new UserJpaEntity(
                userId.value(),
                tenantId.value(),
                "Community",
                "Draw",
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
    static class DrawPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
