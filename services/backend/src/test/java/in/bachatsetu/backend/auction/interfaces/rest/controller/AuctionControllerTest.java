package in.bachatsetu.backend.auction.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auction.application.exception.AuctionNotFoundException;
import in.bachatsetu.backend.auction.application.exception.InvalidBidAmountException;
import in.bachatsetu.backend.auction.application.exception.MemberNotEligibleException;
import in.bachatsetu.backend.auction.application.query.AuctionResult;
import in.bachatsetu.backend.auction.application.query.AuctionSummary;
import in.bachatsetu.backend.auction.application.query.AuctionWinnerResult;
import in.bachatsetu.backend.auction.application.usecase.CloseAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.CreateAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.GetAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.GetWinnerUseCase;
import in.bachatsetu.backend.auction.application.usecase.ListAuctionsUseCase;
import in.bachatsetu.backend.auction.application.usecase.PlaceBidUseCase;
import in.bachatsetu.backend.auction.interfaces.rest.exception.AuctionExceptionHandler;
import in.bachatsetu.backend.auction.interfaces.rest.mapper.AuctionApiMapper;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.draw.application.exception.DrawAccessDeniedException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuctionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AuctionApiMapper.class, AuctionExceptionHandler.class})
class AuctionControllerTest {

    private static final AggregateId TENANT_ID = AggregateId.newId();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateAuctionUseCase createAuction;

    @MockBean
    private PlaceBidUseCase placeBid;

    @MockBean
    private CloseAuctionUseCase closeAuction;

    @MockBean
    private GetAuctionUseCase getAuction;

    @MockBean
    private ListAuctionsUseCase listAuctions;

    @MockBean
    private GetWinnerUseCase getWinner;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsAuction() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID auctionId = UUID.randomUUID();
        when(createAuction.execute(any())).thenReturn(result(auctionId, "OPEN"));

        mockMvc.perform(post("/api/v1/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/auctions/" + auctionId))
                .andExpect(jsonPath("$.auctionId").value(auctionId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void rejectsInvalidGroupIdFormat() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": "not-a-uuid", "cycleId": "%s", "auctionNumber": 1}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void reportsNonOwnerCreateAttemptAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(createAuction.execute(any()))
                .thenThrow(new DrawAccessDeniedException("only the group owner may perform this operation"));

        mockMvc.perform(post("/api/v1/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void rejectsUnauthenticatedCreateRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void placesABid() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID auctionId = UUID.randomUUID();
        when(placeBid.execute(any())).thenReturn(new AuctionBidResult(
                UUID.randomUUID(), UUID.randomUUID(), 10_000L, "INR",
                Instant.parse("2026-07-08T08:00:00Z"), "LEADING"));

        mockMvc.perform(post("/api/v1/auctions/" + auctionId + "/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"discountAmountPaise": 10000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("LEADING"));
    }

    @Test
    void reportsIneligibleMemberBidAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(placeBid.execute(any()))
                .thenThrow(new MemberNotEligibleException("member is not an active participant of this group"));

        mockMvc.perform(post("/api/v1/auctions/" + UUID.randomUUID() + "/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"discountAmountPaise": 10000}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("member-not-eligible"));
    }

    @Test
    void reportsExcessiveBidAmountAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(placeBid.execute(any()))
                .thenThrow(new InvalidBidAmountException("bid discount must not exceed the group's contribution amount"));

        mockMvc.perform(post("/api/v1/auctions/" + UUID.randomUUID() + "/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"discountAmountPaise": 999999999}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("invalid-bid-amount"));
    }

    @Test
    void reportsMissingAuctionOnBidAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(placeBid.execute(any())).thenThrow(new AuctionNotFoundException("auction does not exist"));

        mockMvc.perform(post("/api/v1/auctions/" + UUID.randomUUID() + "/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"discountAmountPaise": 10000}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("auction-not-found"));
    }

    @Test
    void rejectsUnauthenticatedBidRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/auctions/" + UUID.randomUUID() + "/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"discountAmountPaise": 10000}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void closesAuctionWithWinner() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID auctionId = UUID.randomUUID();
        when(closeAuction.execute(any())).thenReturn(result(auctionId, "COMPLETED"));

        mockMvc.perform(post("/api/v1/auctions/" + auctionId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void reportsMissingAuctionOnCloseAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(closeAuction.execute(any())).thenThrow(new AuctionNotFoundException("auction does not exist"));

        mockMvc.perform(post("/api/v1/auctions/" + UUID.randomUUID() + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("auction-not-found"));
    }

    @Test
    void rejectsUnauthenticatedCloseRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/auctions/" + UUID.randomUUID() + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"winnerId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void getsAuction() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID auctionId = UUID.randomUUID();
        when(getAuction.execute(eq(TENANT_ID), any())).thenReturn(result(auctionId, "OPEN"));

        mockMvc.perform(get("/api/v1/auctions/" + auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId").value(auctionId.toString()));
    }

    @Test
    void reportsMissingAuctionAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getAuction.execute(eq(TENANT_ID), any()))
                .thenThrow(new AuctionNotFoundException("auction does not exist"));

        mockMvc.perform(get("/api/v1/auctions/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("auction-not-found"));
    }

    @Test
    void rejectsUnauthenticatedGetRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/auctions/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void listsAuctionsWithDefaultPagination() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        DrawPageRequest expectedRequest = new DrawPageRequest(0, 20, DrawSortField.CREATED_AT, SortDirection.ASC);
        when(listAuctions.execute(TENANT_ID, expectedRequest))
                .thenReturn(new DrawPage<>(List.of(summary(), summary()), 0, 20, 2));

        mockMvc.perform(get("/api/v1/auctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void rejectsInvalidSortAndPaginationParameters() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(get("/api/v1/auctions").param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/auctions").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void rejectsUnauthenticatedListRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/auctions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void getsWinner() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID auctionId = UUID.randomUUID();
        when(getWinner.execute(eq(TENANT_ID), any())).thenReturn(new AuctionWinnerResult(
                auctionId, UUID.randomUUID(), 15_000L, "INR", Instant.parse("2026-07-08T08:00:00Z")));

        mockMvc.perform(get("/api/v1/auctions/" + auctionId + "/winner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winningDiscountAmountPaise").value(15_000));
    }

    @Test
    void reportsMissingWinnerAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getWinner.execute(eq(TENANT_ID), any()))
                .thenThrow(new AuctionNotFoundException("auction has no recorded winner"));

        mockMvc.perform(get("/api/v1/auctions/" + UUID.randomUUID() + "/winner"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("auction-not-found"));
    }

    @Test
    void rejectsUnauthenticatedWinnerRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/auctions/" + UUID.randomUUID() + "/winner"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                TENANT_ID,
                Set.of("GROUP_MEMBER"),
                Set.of("auction.read"));
    }

    private AuctionResult result(UUID auctionId, String status) {
        Instant now = Instant.parse("2026-07-08T08:00:00Z");
        return new AuctionResult(
                auctionId, TENANT_ID.value(), UUID.randomUUID(), UUID.randomUUID(), 1, status,
                now.plusSeconds(3600), null, List.of(), now, now, 0);
    }

    private AuctionSummary summary() {
        return new AuctionSummary(UUID.randomUUID(), 1, "OPEN", Instant.parse("2026-07-08T08:00:00Z"), null);
    }

    private String validCreateRequestBody() {
        return """
                {
                  "groupId": "%s",
                  "cycleId": "%s",
                  "auctionNumber": 1
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
