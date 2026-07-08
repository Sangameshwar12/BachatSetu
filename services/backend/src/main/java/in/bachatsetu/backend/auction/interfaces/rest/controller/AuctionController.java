package in.bachatsetu.backend.auction.interfaces.rest.controller;

import in.bachatsetu.backend.auction.application.query.AuctionResult;
import in.bachatsetu.backend.auction.application.query.AuctionWinnerResult;
import in.bachatsetu.backend.auction.application.usecase.CloseAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.CreateAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.GetAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.GetWinnerUseCase;
import in.bachatsetu.backend.auction.application.usecase.ListAuctionsUseCase;
import in.bachatsetu.backend.auction.application.usecase.PlaceBidUseCase;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionBidResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionSummaryResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.AuctionWinnerResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.CloseAuctionRequest;
import in.bachatsetu.backend.auction.interfaces.rest.dto.CreateAuctionRequest;
import in.bachatsetu.backend.auction.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.auction.interfaces.rest.dto.PlaceBidRequest;
import in.bachatsetu.backend.auction.interfaces.rest.mapper.AuctionApiMapper;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.draw.application.query.AuctionBidResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes Auction use cases without leaking domain or persistence models. */
@RestController
@Validated
@RequestMapping(path = "/api/v1/auctions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auctions", description = "Schedule auctions, place bids, and record winners")
@ConditionalOnProperty(
        prefix = "bachatsetu.auction.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuctionController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CreateAuctionUseCase createAuction;
    private final PlaceBidUseCase placeBid;
    private final CloseAuctionUseCase closeAuction;
    private final GetAuctionUseCase getAuction;
    private final ListAuctionsUseCase listAuctions;
    private final GetWinnerUseCase getWinner;
    private final CurrentUserProvider currentUserProvider;
    private final AuctionApiMapper mapper;

    public AuctionController(
            CreateAuctionUseCase createAuction,
            PlaceBidUseCase placeBid,
            CloseAuctionUseCase closeAuction,
            GetAuctionUseCase getAuction,
            ListAuctionsUseCase listAuctions,
            GetWinnerUseCase getWinner,
            CurrentUserProvider currentUserProvider,
            AuctionApiMapper mapper) {
        this.createAuction = createAuction;
        this.placeBid = placeBid;
        this.closeAuction = closeAuction;
        this.getAuction = getAuction;
        this.listAuctions = listAuctions;
        this.getWinner = getWinner;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Schedule an auction",
            description = "Creates a new auction for a savings group cycle, opened immediately for bidding.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Auction scheduled"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuctionResponse> create(@Valid @RequestBody CreateAuctionRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        AuctionResult result = createAuction.execute(mapper.toCreateCommand(request, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/auctions/" + result.auctionId()))
                .body(mapper.toResponse(result));
    }

    @PostMapping(path = "/{auctionId}/bids", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Place a bid",
            description = "Places the authenticated member's discount bid against an open auction.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Bid placed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuctionBidResponse> placeBid(
            @PathVariable String auctionId,
            @Valid @RequestBody PlaceBidRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        AuctionBidResult result = placeBid.execute(mapper.toPlaceBidCommand(auctionId, request, currentUser));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toBidResponse(result));
    }

    @PostMapping(path = "/{auctionId}/close", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Close an auction", description = "Closes an open auction with its winning member.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Auction closed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Invalid lifecycle transition", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public AuctionResponse close(
            @PathVariable String auctionId,
            @Valid @RequestBody CloseAuctionRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        AuctionResult result = closeAuction.execute(mapper.toCloseCommand(auctionId, request, currentUser));
        return mapper.toResponse(result);
    }

    @GetMapping
    @Operation(
            summary = "List auctions",
            description = "Lists auctions within the authenticated caller's tenant, page by page.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameters", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<AuctionSummaryResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(0)
            @Parameter(description = "Zero-based page index") int page,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            @Parameter(description = "Page size, up to 100") int size,
            @RequestParam(defaultValue = "createdAt")
            @Pattern(regexp = "scheduledAt|createdAt")
            @Parameter(description = "Field to sort by", example = "createdAt") String sort,
            @RequestParam(defaultValue = "asc")
            @Pattern(regexp = "asc|desc")
            @Parameter(description = "Sort direction", example = "asc") String direction) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        return mapper.listAuctions(listAuctions, currentUser, page, size, sort, direction);
    }

    @GetMapping("/{auctionId}")
    @Operation(summary = "Get an auction", description = "Retrieves one tenant-scoped auction.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Auction returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Auction not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public AuctionResponse get(@PathVariable String auctionId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        AuctionResult result = mapper.getAuction(getAuction, currentUser, auctionId);
        return mapper.toResponse(result);
    }

    @GetMapping("/{auctionId}/winner")
    @Operation(summary = "Get the auction winner", description = "Retrieves the winning member of a closed auction.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Winner returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Auction not found or has no winner yet", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public AuctionWinnerResponse winner(@PathVariable String auctionId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        AuctionWinnerResult result = mapper.getWinner(getWinner, currentUser, auctionId);
        return mapper.toWinnerResponse(result);
    }
}
