package in.bachatsetu.backend.infrastructure.persistence.integration;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.draw.domain.model.AuctionWinner;
import in.bachatsetu.backend.draw.domain.model.BidAmount;
import in.bachatsetu.backend.draw.domain.model.BidStatus;
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
@Import(AuctionPersistencePostgreSqlIntegrationTest.AuctionPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AuctionPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f5000000-0000-0000-0000-000000000001");

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
    void tracksTheLeadingBidAcrossMultipleCompetingBidsAndSurvivesReload() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "auction-owner@example.in");
        groupRepository.save(group);
        flushAndClear();
        MonthlyCycleJpaEntity cycle = persistCycle(group, 1);
        AggregateId firstBidder = persistMember(group, "MB-AUCTION0000001", "bidder-one@example.in");
        AggregateId secondBidder = persistMember(group, "MB-AUCTION0000002", "bidder-two@example.in");
        AggregateId thirdBidder = persistMember(group, "MB-AUCTION0000003", "bidder-three@example.in");
        flushAndClear();

        AggregateId auctionId = AggregateId.newId();
        Draw auction = Draw.schedule(
                auctionId, group.tenantId(), group.id(), new AggregateId(cycle.getId()),
                new DrawNumber(1), DrawType.AUCTION, NOW.plusSeconds(3600), group.organizerId(), NOW);
        auction.open(group.organizerId(), NOW.plusSeconds(3600));
        auction.submitBid(firstBidder, new BidAmount(Money.inr(15_000)), group.organizerId(), NOW.plusSeconds(3700));
        auction.submitBid(secondBidder, new BidAmount(Money.inr(30_000)), group.organizerId(), NOW.plusSeconds(3800));
        auction.submitBid(thirdBidder, new BidAmount(Money.inr(20_000)), group.organizerId(), NOW.plusSeconds(3900));
        auction.complete(secondBidder, group.organizerId(), NOW.plusSeconds(4000));

        drawRepository.save(auction);
        flushAndClear();

        Draw restored = drawRepository.findById(group.tenantId(), auctionId).orElseThrow();
        assertThat(restored.status().name()).isEqualTo("COMPLETED");
        assertThat(restored.winnerMemberId()).isEqualTo(secondBidder);
        assertThat(restored.bids()).hasSize(3);
        assertThat(restored.bids()).filteredOn(bid -> bid.memberId().equals(secondBidder))
                .singleElement()
                .satisfies(bid -> assertThat(bid.status()).isEqualTo(BidStatus.ACCEPTED));
        assertThat(restored.bids()).filteredOn(bid -> !bid.memberId().equals(secondBidder))
                .allSatisfy(bid -> assertThat(bid.status()).isEqualTo(BidStatus.OUTBID));

        AuctionWinner winner = restored.winner().orElseThrow();
        assertThat(winner.memberId()).isEqualTo(secondBidder);
        assertThat(winner.winningAmount().discount()).isEqualTo(Money.inr(30_000));
    }

    @Test
    @Transactional
    void findsOnlyAuctionTypeDrawsWhenPaginatingByType() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "auction-owner-two@example.in");
        AggregateId tenantId = group.tenantId();
        groupRepository.save(group);
        flushAndClear();

        Draw randomDraw = newScheduledDraw(group, 1, DrawType.RANDOM, NOW.plusSeconds(3600));
        Draw firstAuction = newScheduledDraw(group, 2, DrawType.AUCTION, NOW.plusSeconds(7200));
        Draw secondAuction = newScheduledDraw(group, 3, DrawType.AUCTION, NOW.plusSeconds(10_800));
        Draw rotationDraw = newScheduledDraw(group, 4, DrawType.FIXED_ROTATION, NOW.plusSeconds(14_400));
        drawRepository.save(randomDraw);
        drawRepository.save(firstAuction);
        drawRepository.save(secondAuction);
        drawRepository.save(rotationDraw);
        flushAndClear();

        DrawPage<Draw> auctionsAscending = drawRepository.findPageByType(
                tenantId, DrawType.AUCTION, new DrawPageRequest(0, 10, DrawSortField.SCHEDULED_AT, SortDirection.ASC));

        assertThat(auctionsAscending.totalElements()).isEqualTo(2);
        assertThat(auctionsAscending.content()).extracting(draw -> draw.number().value())
                .containsExactly(2, 3);
        assertThat(auctionsAscending.content()).allSatisfy(
                draw -> assertThat(draw.type()).isEqualTo(DrawType.AUCTION));

        DrawPage<Draw> firstPageOfOne = drawRepository.findPageByType(
                tenantId, DrawType.AUCTION, new DrawPageRequest(0, 1, DrawSortField.SCHEDULED_AT, SortDirection.ASC));
        assertThat(firstPageOfOne.content()).extracting(draw -> draw.number().value()).containsExactly(2);
        assertThat(firstPageOfOne.totalElements()).isEqualTo(2);
        assertThat(firstPageOfOne.hasNext()).isTrue();
    }

    private Draw newScheduledDraw(SavingsGroup group, int cycleNumber, DrawType type, java.time.Instant scheduledAt) {
        MonthlyCycleJpaEntity cycle = persistCycle(group, cycleNumber);
        return Draw.schedule(
                AggregateId.newId(), group.tenantId(), group.id(), new AggregateId(cycle.getId()),
                new DrawNumber(cycleNumber), type, scheduledAt, group.organizerId(), NOW);
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
                "Auction",
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
    static class AuctionPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
