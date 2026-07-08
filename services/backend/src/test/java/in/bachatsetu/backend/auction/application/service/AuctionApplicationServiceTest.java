package in.bachatsetu.backend.auction.application.service;

import static in.bachatsetu.backend.auction.application.ApplicationTestFixture.NOW;
import static in.bachatsetu.backend.auction.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.auction.application.ApplicationTestFixture.directTransaction;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auction.application.command.CloseAuctionCommand;
import in.bachatsetu.backend.auction.application.command.CreateAuctionCommand;
import in.bachatsetu.backend.auction.application.command.PlaceBidCommand;
import in.bachatsetu.backend.auction.application.exception.AuctionAccessDeniedException;
import in.bachatsetu.backend.auction.application.exception.AuctionNotFoundException;
import in.bachatsetu.backend.auction.application.exception.InvalidBidAmountException;
import in.bachatsetu.backend.auction.application.exception.MemberNotEligibleException;
import in.bachatsetu.backend.auction.application.mapper.AuctionApplicationMapper;
import in.bachatsetu.backend.auction.application.port.ClockPort;
import in.bachatsetu.backend.auction.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.auction.application.query.AuctionResult;
import in.bachatsetu.backend.auction.application.query.AuctionSummary;
import in.bachatsetu.backend.auction.application.query.AuctionWinnerResult;
import in.bachatsetu.backend.auction.application.usecase.CloseAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.CreateAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.GetAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.GetWinnerUseCase;
import in.bachatsetu.backend.auction.application.usecase.ListAuctionsUseCase;
import in.bachatsetu.backend.auction.application.usecase.PlaceBidUseCase;
import in.bachatsetu.backend.draw.application.exception.DrawAccessDeniedException;
import in.bachatsetu.backend.draw.application.query.AuctionBidResult;
import in.bachatsetu.backend.draw.application.security.DrawAuthorizationService;
import in.bachatsetu.backend.draw.domain.exception.DuplicateBidException;
import in.bachatsetu.backend.draw.domain.exception.InvalidDrawStateException;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.draw.domain.port.DrawSortField;
import in.bachatsetu.backend.draw.domain.port.SortDirection;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuctionApplicationServiceTest {

    private DrawRepository repository;
    private SavingsGroupRepository groupRepository;
    private DomainEventPublisherPort publisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private AuctionApplicationMapper mapper;
    private DrawAuthorizationService authorization;

    @BeforeEach
    void setUp() {
        repository = mock(DrawRepository.class);
        groupRepository = mock(SavingsGroupRepository.class);
        publisher = mock(DomainEventPublisherPort.class);
        clock = () -> NOW.plusSeconds(60);
        transaction = directTransaction();
        mapper = new AuctionApplicationMapper();
        authorization = new DrawAuthorizationService();
    }

    @Test
    void createsAnAuctionAlreadyOpenForBidding() {
        CreateAuctionCommand command = createCommand();
        SavingsGroup group = newGroup(command.actorId(), 5);
        when(groupRepository.findById(command.tenantId(), new GroupId(command.groupId())))
                .thenReturn(Optional.of(group));
        CreateAuctionUseCase service = new CreateAuctionApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization);

        AuctionResult result = service.execute(command);

        assertThat(result.status()).isEqualTo("OPEN");
        verify(repository).save(any(Draw.class));
    }

    @Test
    void createRejectsAnActorWhoIsNotTheGroupOwner() {
        CreateAuctionCommand command = createCommand();
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        when(groupRepository.findById(command.tenantId(), new GroupId(command.groupId())))
                .thenReturn(Optional.of(group));
        CreateAuctionUseCase service = new CreateAuctionApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(DrawAccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsWhenTheTargetGroupDoesNotExist() {
        CreateAuctionCommand command = createCommand();
        when(groupRepository.findById(command.tenantId(), new GroupId(command.groupId())))
                .thenReturn(Optional.empty());
        CreateAuctionUseCase service = new CreateAuctionApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(AuctionAccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void placesAnEligibleMembersBid() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW.plusSeconds(1));
        AggregateId memberId = AggregateId.newId();
        group.joinMember(memberId, ownerId, NOW.plusSeconds(2));
        Draw auction = newOpenAuction(ownerId, group.id());
        when(repository.findById(auction.tenantId(), auction.id())).thenReturn(Optional.of(auction));
        when(groupRepository.findById(auction.tenantId(), new GroupId(auction.groupId())))
                .thenReturn(Optional.of(group));
        PlaceBidUseCase service = new PlaceBidApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper);

        AuctionBidResult result = service.execute(new PlaceBidCommand(
                auction.tenantId(), auction.id(), memberId, Money.inr(50_000), memberId));

        assertThat(result.memberId()).isEqualTo(memberId.value());
        assertThat(result.status()).isEqualTo("LEADING");
        verify(repository).save(auction);
    }

    @Test
    void placeBidRejectsAnIneligibleMember() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        AggregateId outsiderId = AggregateId.newId();
        Draw auction = newOpenAuction(ownerId, group.id());
        when(repository.findById(auction.tenantId(), auction.id())).thenReturn(Optional.of(auction));
        when(groupRepository.findById(auction.tenantId(), new GroupId(auction.groupId())))
                .thenReturn(Optional.of(group));
        PlaceBidUseCase service = new PlaceBidApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(new PlaceBidCommand(
                        auction.tenantId(), auction.id(), outsiderId, Money.inr(50_000), outsiderId)))
                .isInstanceOf(MemberNotEligibleException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void placeBidRejectsAnAmountExceedingTheGroupsContributionAmount() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        Draw auction = newOpenAuction(ownerId, group.id());
        when(repository.findById(auction.tenantId(), auction.id())).thenReturn(Optional.of(auction));
        when(groupRepository.findById(auction.tenantId(), new GroupId(auction.groupId())))
                .thenReturn(Optional.of(group));
        PlaceBidUseCase service = new PlaceBidApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper);
        Money tooHigh = group.contributionAmount().value().add(Money.inr(1));

        assertThatThrownBy(() -> service.execute(new PlaceBidCommand(
                        auction.tenantId(), auction.id(), ownerId, tooHigh, ownerId)))
                .isInstanceOf(InvalidBidAmountException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void placeBidRejectsADuplicateActiveBidFromTheSameMember() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        Draw auction = newOpenAuction(ownerId, group.id());
        auction.submitBid(ownerId, new in.bachatsetu.backend.draw.domain.model.BidAmount(Money.inr(10_000)),
                ownerId, NOW.plusSeconds(30));
        when(repository.findById(auction.tenantId(), auction.id())).thenReturn(Optional.of(auction));
        when(groupRepository.findById(auction.tenantId(), new GroupId(auction.groupId())))
                .thenReturn(Optional.of(group));
        PlaceBidUseCase service = new PlaceBidApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(new PlaceBidCommand(
                        auction.tenantId(), auction.id(), ownerId, Money.inr(20_000), ownerId)))
                .isInstanceOf(DuplicateBidException.class);
    }

    @Test
    void placeBidRejectsBiddingAfterTheAuctionHasClosed() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        Draw auction = newOpenAuction(ownerId, group.id());
        auction.submitBid(ownerId, new in.bachatsetu.backend.draw.domain.model.BidAmount(Money.inr(10_000)),
                ownerId, NOW.plusSeconds(30));
        auction.complete(ownerId, ownerId, NOW.plusSeconds(60));
        when(repository.findById(auction.tenantId(), auction.id())).thenReturn(Optional.of(auction));
        when(groupRepository.findById(auction.tenantId(), new GroupId(auction.groupId())))
                .thenReturn(Optional.of(group));
        PlaceBidUseCase service = new PlaceBidApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(new PlaceBidCommand(
                        auction.tenantId(), auction.id(), ownerId, Money.inr(10_000), ownerId)))
                .isInstanceOf(InvalidDrawStateException.class);
    }

    @Test
    void placeBidPropagatesNotFoundForAMissingAuction() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId auctionId = AggregateId.newId();
        when(repository.findById(tenantId, auctionId)).thenReturn(Optional.empty());
        PlaceBidUseCase service = new PlaceBidApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(new PlaceBidCommand(
                        tenantId, auctionId, AggregateId.newId(), Money.inr(10_000), AggregateId.newId())))
                .isInstanceOf(AuctionNotFoundException.class);
    }

    @Test
    void placeBidPropagatesNotFoundForANonAuctionDraw() {
        AggregateId ownerId = AggregateId.newId();
        Draw randomDraw = Draw.schedule(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new DrawNumber(1), DrawType.RANDOM, NOW.plusSeconds(10), ownerId, NOW);
        when(repository.findById(randomDraw.tenantId(), randomDraw.id())).thenReturn(Optional.of(randomDraw));
        PlaceBidUseCase service = new PlaceBidApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(new PlaceBidCommand(
                        randomDraw.tenantId(), randomDraw.id(), ownerId, Money.inr(10_000), ownerId)))
                .isInstanceOf(AuctionNotFoundException.class);
    }

    @Test
    void closesAnOpenAuctionWithItsWinner() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        Draw auction = newOpenAuction(ownerId, group.id());
        auction.submitBid(ownerId, new in.bachatsetu.backend.draw.domain.model.BidAmount(Money.inr(10_000)),
                ownerId, NOW.plusSeconds(30));
        when(repository.findById(auction.tenantId(), auction.id())).thenReturn(Optional.of(auction));
        when(groupRepository.findById(auction.tenantId(), new GroupId(auction.groupId())))
                .thenReturn(Optional.of(group));
        CloseAuctionUseCase service = new CloseAuctionApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization);

        AuctionResult result = service.execute(
                new CloseAuctionCommand(auction.tenantId(), auction.id(), ownerId, ownerId));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.winnerMemberId()).isEqualTo(ownerId.value());
        verify(repository).save(auction);
        verify(publisher).publish(any());
    }

    @Test
    void closeRejectsAnActorWhoIsNotTheGroupOwner() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        Draw auction = newOpenAuction(ownerId, group.id());
        auction.submitBid(ownerId, new in.bachatsetu.backend.draw.domain.model.BidAmount(Money.inr(10_000)),
                ownerId, NOW.plusSeconds(30));
        when(repository.findById(auction.tenantId(), auction.id())).thenReturn(Optional.of(auction));
        when(groupRepository.findById(auction.tenantId(), new GroupId(auction.groupId())))
                .thenReturn(Optional.of(group));
        CloseAuctionUseCase service = new CloseAuctionApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(
                        new CloseAuctionCommand(auction.tenantId(), auction.id(), ownerId, AggregateId.newId())))
                .isInstanceOf(DrawAccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void closePropagatesNotFoundForAMissingAuction() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId auctionId = AggregateId.newId();
        when(repository.findById(tenantId, auctionId)).thenReturn(Optional.empty());
        CloseAuctionUseCase service = new CloseAuctionApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(new CloseAuctionCommand(
                        tenantId, auctionId, AggregateId.newId(), AggregateId.newId())))
                .isInstanceOf(AuctionNotFoundException.class);
    }

    @Test
    void retrievesTenantScopedAuction() {
        AggregateId tenantId = AggregateId.newId();
        Draw auction = newOpenAuction(AggregateId.newId(), AggregateId.newId());
        when(repository.findById(tenantId, auction.id())).thenReturn(Optional.of(auction));
        GetAuctionUseCase service = new GetAuctionApplicationService(repository, transaction, mapper);

        AuctionResult result = service.execute(tenantId, auction.id());

        assertThat(result.auctionId()).isEqualTo(auction.id().value());
    }

    @Test
    void getExcludesANonAuctionDraw() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId ownerId = AggregateId.newId();
        Draw randomDraw = Draw.schedule(
                AggregateId.newId(), tenantId, AggregateId.newId(), AggregateId.newId(),
                new DrawNumber(1), DrawType.RANDOM, NOW.plusSeconds(10), ownerId, NOW);
        when(repository.findById(tenantId, randomDraw.id())).thenReturn(Optional.of(randomDraw));
        GetAuctionUseCase service = new GetAuctionApplicationService(repository, transaction, mapper);

        assertThatThrownBy(() -> service.execute(tenantId, randomDraw.id()))
                .isInstanceOf(AuctionNotFoundException.class);
    }

    @Test
    void tenantScopedLookupHidesAuctionsFromOtherTenants() {
        AggregateId tenantId = AggregateId.newId();
        Draw auction = newOpenAuction(AggregateId.newId(), AggregateId.newId());
        when(repository.findById(tenantId, auction.id())).thenReturn(Optional.empty());
        GetAuctionUseCase service = new GetAuctionApplicationService(repository, transaction, mapper);

        assertThatThrownBy(() -> service.execute(tenantId, auction.id()))
                .isInstanceOf(AuctionNotFoundException.class);
    }

    @Test
    void listsTenantScopedAuctionSummariesUsingTheTypeFilteredQuery() {
        AggregateId tenantId = AggregateId.newId();
        Draw first = newOpenAuction(AggregateId.newId(), AggregateId.newId());
        Draw second = newOpenAuction(AggregateId.newId(), AggregateId.newId());
        DrawPageRequest pageRequest = new DrawPageRequest(0, 20, DrawSortField.CREATED_AT, SortDirection.ASC);
        when(repository.findPageByType(tenantId, DrawType.AUCTION, pageRequest))
                .thenReturn(new DrawPage<>(List.of(first, second), 0, 20, 2));
        ListAuctionsUseCase service = new ListAuctionsApplicationService(repository, transaction, mapper);

        DrawPage<AuctionSummary> page = service.execute(tenantId, pageRequest);

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void getsTheWinnerOfAClosedAuction() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId ownerId = AggregateId.newId();
        Draw auction = newOpenAuction(ownerId, AggregateId.newId());
        auction.submitBid(ownerId, new in.bachatsetu.backend.draw.domain.model.BidAmount(Money.inr(10_000)),
                ownerId, NOW.plusSeconds(30));
        auction.complete(ownerId, ownerId, NOW.plusSeconds(60));
        when(repository.findById(tenantId, auction.id())).thenReturn(Optional.of(auction));
        GetWinnerUseCase service = new GetWinnerApplicationService(repository, transaction, mapper);

        AuctionWinnerResult result = service.execute(tenantId, auction.id());

        assertThat(result.memberId()).isEqualTo(ownerId.value());
        assertThat(result.winningDiscountAmountPaise()).isEqualTo(10_000L);
    }

    @Test
    void getWinnerPropagatesNotFoundWhenTheAuctionHasNoWinnerYet() {
        AggregateId tenantId = AggregateId.newId();
        Draw auction = newOpenAuction(AggregateId.newId(), AggregateId.newId());
        when(repository.findById(tenantId, auction.id())).thenReturn(Optional.of(auction));
        GetWinnerUseCase service = new GetWinnerApplicationService(repository, transaction, mapper);

        assertThatThrownBy(() -> service.execute(tenantId, auction.id()))
                .isInstanceOf(AuctionNotFoundException.class);
    }

    @Test
    void rejectsNullUseCaseInputs() {
        assertThatThrownBy(() -> new CreateAuctionApplicationService(
                        repository, groupRepository, publisher, clock, transaction, mapper, authorization)
                        .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PlaceBidApplicationService(
                        repository, groupRepository, publisher, clock, transaction, mapper)
                        .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseAuctionApplicationService(
                        repository, groupRepository, publisher, clock, transaction, mapper, authorization)
                        .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetAuctionApplicationService(repository, transaction, mapper)
                        .execute(null, AggregateId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListAuctionsApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWinnerApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new CreateAuctionApplicationService(
                        null, groupRepository, publisher, clock, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PlaceBidApplicationService(
                        null, groupRepository, publisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseAuctionApplicationService(
                        null, groupRepository, publisher, clock, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetAuctionApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListAuctionsApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetWinnerApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
    }

    private Draw newOpenAuction(AggregateId actorId, AggregateId groupId) {
        Draw auction = Draw.schedule(
                AggregateId.newId(), AggregateId.newId(), groupId, AggregateId.newId(),
                new DrawNumber(1), DrawType.AUCTION, NOW, actorId, NOW);
        auction.open(actorId, NOW);
        return auction;
    }
}
