package in.bachatsetu.backend.auction.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auction.application.command.CloseAuctionCommand;
import in.bachatsetu.backend.auction.application.command.CreateAuctionCommand;
import in.bachatsetu.backend.auction.application.command.PlaceBidCommand;
import in.bachatsetu.backend.auction.application.query.AuctionResult;
import in.bachatsetu.backend.auction.application.query.AuctionSummary;
import in.bachatsetu.backend.auction.application.query.AuctionWinnerResult;
import in.bachatsetu.backend.auction.application.usecase.GetAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.GetWinnerUseCase;
import in.bachatsetu.backend.auction.application.usecase.ListAuctionsUseCase;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionSummaryResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionWinnerResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.CloseAuctionRequest;
import in.bachatsetu.backend.auction.interfaces.rest.dto.CreateAuctionRequest;
import in.bachatsetu.backend.auction.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.PlaceBidRequest;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.draw.application.query.AuctionBidResult;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawSortField;
import in.bachatsetu.backend.draw.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuctionApiMapperTest {

    private final AuctionApiMapper mapper = new AuctionApiMapper();

    @Test
    void mapsCreateRequestToCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID groupId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();
        CreateAuctionRequest request = new CreateAuctionRequest(groupId.toString(), cycleId.toString(), 1);

        CreateAuctionCommand command = mapper.toCreateCommand(request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.groupId().value()).isEqualTo(groupId);
        assertThat(command.cycleId().value()).isEqualTo(cycleId);
        assertThat(command.number().value()).isEqualTo(1);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void mapsPlaceBidRequestUsingTheAuthenticatedCallerAsTheBiddingMember() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID auctionId = UUID.randomUUID();
        PlaceBidRequest request = new PlaceBidRequest(10_000L);

        PlaceBidCommand command = mapper.toPlaceBidCommand(auctionId.toString(), request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.auctionId().value()).isEqualTo(auctionId);
        assertThat(command.memberId()).isEqualTo(currentUser.userId().toAggregateId());
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
        assertThat(command.discountAmount().minorUnits()).isEqualTo(10_000L);
    }

    @Test
    void mapsCloseCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID auctionId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        CloseAuctionRequest request = new CloseAuctionRequest(winnerId.toString());

        CloseAuctionCommand command = mapper.toCloseCommand(auctionId.toString(), request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.auctionId().value()).isEqualTo(auctionId);
        assertThat(command.winnerId().value()).isEqualTo(winnerId);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void getAuctionDelegatesToUseCaseWithParsedIdentifiers() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID auctionId = UUID.randomUUID();
        AuctionResult expected = result(auctionId, "OPEN");
        GetAuctionUseCase useCase = (tenantId, id) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(id.value()).isEqualTo(auctionId);
            return expected;
        };

        assertThat(mapper.getAuction(useCase, currentUser, auctionId.toString())).isEqualTo(expected);
    }

    @Test
    void getWinnerDelegatesToUseCaseWithParsedIdentifiers() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID auctionId = UUID.randomUUID();
        AuctionWinnerResult expected = new AuctionWinnerResult(
                auctionId, UUID.randomUUID(), 10_000L, "INR", Instant.parse("2026-07-08T08:00:00Z"));
        GetWinnerUseCase useCase = (tenantId, id) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(id.value()).isEqualTo(auctionId);
            return expected;
        };

        assertThat(mapper.getWinner(useCase, currentUser, auctionId.toString())).isEqualTo(expected);
    }

    @Test
    void mapsResultToResponseIncludingBids() {
        UUID auctionId = UUID.randomUUID();
        AuctionResult result = result(auctionId, "COMPLETED");

        AuctionResponse response = mapper.toResponse(result);

        assertThat(response.auctionId()).isEqualTo(auctionId.toString());
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.bids()).singleElement()
                .satisfies(bid -> assertThat(bid.status()).isEqualTo("LEADING"));
    }

    @Test
    void mapsWinnerToResponse() {
        AuctionWinnerResult winner = new AuctionWinnerResult(
                UUID.randomUUID(), UUID.randomUUID(), 12_000L, "INR", Instant.parse("2026-07-08T08:00:00Z"));

        AuctionWinnerResponse response = mapper.toWinnerResponse(winner);

        assertThat(response.winningDiscountAmountPaise()).isEqualTo(12_000L);
        assertThat(response.currencyCode()).isEqualTo("INR");
    }

    @Test
    void buildsPageRequestFromValidatedRestParameters() {
        DrawPageRequest pageRequest = mapper.toPageRequest(1, 10, "scheduledAt", "desc");

        assertThat(pageRequest.page()).isEqualTo(1);
        assertThat(pageRequest.size()).isEqualTo(10);
        assertThat(pageRequest.sortField()).isEqualTo(DrawSortField.SCHEDULED_AT);
        assertThat(pageRequest.direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void rejectsUnsupportedSortOrDirectionValues() {
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "unsupported", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "scheduledAt", "sideways"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listAuctionsConsolidatesPageRequestAndResponseForTheController() {
        AuthenticatedUser currentUser = authenticatedUser();
        ListAuctionsUseCase useCase = (tenantId, request) -> new DrawPage<>(List.of(summary()), 0, 20, 1);

        PageResponse<AuctionSummaryResponse> response =
                mapper.listAuctions(useCase, currentUser, 0, 20, "createdAt", "asc");

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void mapsAuctionPageToPageResponse() {
        DrawPage<AuctionSummary> page = new DrawPage<>(List.of(summary(), summary()), 0, 2, 3);

        PageResponse<AuctionSummaryResponse> response = mapper.toSummaryPage(page);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("auction.read"));
    }

    private AuctionResult result(UUID auctionId, String status) {
        Instant now = Instant.parse("2026-07-08T08:00:00Z");
        UUID winnerId = UUID.randomUUID();
        return new AuctionResult(
                auctionId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, status,
                now.plusSeconds(3600), winnerId,
                List.of(new AuctionBidResult(UUID.randomUUID(), winnerId, 10_000L, "INR", now.plusSeconds(3700), "LEADING")),
                now, now, 0);
    }

    private AuctionSummary summary() {
        return new AuctionSummary(UUID.randomUUID(), 1, "OPEN", Instant.parse("2026-07-08T08:00:00Z"), null);
    }
}
