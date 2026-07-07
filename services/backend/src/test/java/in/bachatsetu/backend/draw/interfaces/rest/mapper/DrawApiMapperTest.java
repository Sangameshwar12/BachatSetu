package in.bachatsetu.backend.draw.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.draw.application.command.CloseDrawCommand;
import in.bachatsetu.backend.draw.application.command.ConductDrawCommand;
import in.bachatsetu.backend.draw.application.command.CreateDrawCommand;
import in.bachatsetu.backend.draw.application.query.AuctionBidResult;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.query.DrawSummary;
import in.bachatsetu.backend.draw.application.usecase.GetDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.ListDrawsUseCase;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawSortField;
import in.bachatsetu.backend.draw.domain.port.SortDirection;
import in.bachatsetu.backend.draw.interfaces.rest.dto.CloseDrawRequest;
import in.bachatsetu.backend.draw.interfaces.rest.dto.CreateDrawRequest;
import in.bachatsetu.backend.draw.interfaces.rest.dto.DrawResponse;
import in.bachatsetu.backend.draw.interfaces.rest.dto.DrawSummaryResponse;
import in.bachatsetu.backend.draw.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawApiMapperTest {

    private final DrawApiMapper mapper = new DrawApiMapper();

    @Test
    void mapsCreateRequestToCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID groupId = UUID.randomUUID();
        UUID cycleId = UUID.randomUUID();
        Instant scheduledAt = Instant.parse("2026-08-01T10:00:00Z");
        CreateDrawRequest request =
                new CreateDrawRequest(groupId.toString(), cycleId.toString(), 1, "AUCTION", scheduledAt);

        CreateDrawCommand command = mapper.toCreateCommand(request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.groupId().value()).isEqualTo(groupId);
        assertThat(command.cycleId().value()).isEqualTo(cycleId);
        assertThat(command.number().value()).isEqualTo(1);
        assertThat(command.type().name()).isEqualTo("AUCTION");
        assertThat(command.scheduledAt()).isEqualTo(scheduledAt);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void getDrawDelegatesToUseCaseWithParsedIdentifiers() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID drawId = UUID.randomUUID();
        DrawResult expected = result(drawId, "OPEN");
        GetDrawUseCase useCase = (tenantId, id) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(id.value()).isEqualTo(drawId);
            return expected;
        };

        assertThat(mapper.getDraw(useCase, currentUser, drawId.toString())).isEqualTo(expected);
    }

    @Test
    void mapsResultToResponseIncludingBids() {
        UUID drawId = UUID.randomUUID();
        DrawResult result = result(drawId, "COMPLETED");

        DrawResponse response = mapper.toResponse(result);

        assertThat(response.drawId()).isEqualTo(drawId.toString());
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.winnerMemberId()).isEqualTo(result.winnerMemberId().toString());
        assertThat(response.bids()).singleElement()
                .satisfies(bid -> assertThat(bid.status()).isEqualTo("LEADING"));
    }

    @Test
    void mapsResultWithoutWinnerToNullWinnerId() {
        DrawResult result = resultWithoutWinner();

        DrawResponse response = mapper.toResponse(result);

        assertThat(response.winnerMemberId()).isNull();
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
    void listDrawsDelegatesToUseCaseWithTenantIdentityAndPageRequest() {
        AuthenticatedUser currentUser = authenticatedUser();
        DrawPageRequest pageRequest = new DrawPageRequest(0, 20, DrawSortField.CREATED_AT, SortDirection.ASC);
        DrawPage<DrawSummary> expected = new DrawPage<>(List.of(summary()), 0, 20, 1);
        ListDrawsUseCase useCase = (tenantId, request) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(request).isEqualTo(pageRequest);
            return expected;
        };

        assertThat(mapper.listDraws(useCase, currentUser, pageRequest)).isEqualTo(expected);
    }

    @Test
    void listDrawsConsolidatesPageRequestAndResponseForTheController() {
        AuthenticatedUser currentUser = authenticatedUser();
        ListDrawsUseCase useCase = (tenantId, request) -> new DrawPage<>(List.of(summary()), 0, 20, 1);

        PageResponse<DrawSummaryResponse> response =
                mapper.listDraws(useCase, currentUser, 0, 20, "createdAt", "asc");

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void mapsSummaryToResponse() {
        DrawSummaryResponse response = mapper.toSummaryResponse(summary());

        assertThat(response.status()).isEqualTo("SCHEDULED");
    }

    @Test
    void mapsDrawPageToPageResponse() {
        DrawPage<DrawSummary> page = new DrawPage<>(List.of(summary(), summary()), 0, 2, 3);

        PageResponse<DrawSummaryResponse> response = mapper.toSummaryPage(page);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    void mapsConductCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID drawId = UUID.randomUUID();

        ConductDrawCommand command = mapper.toConductCommand(drawId.toString(), currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.drawId().value()).isEqualTo(drawId);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void mapsCloseCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID drawId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        CloseDrawRequest request = new CloseDrawRequest(winnerId.toString());

        CloseDrawCommand command = mapper.toCloseCommand(drawId.toString(), request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.drawId().value()).isEqualTo(drawId);
        assertThat(command.winnerId().value()).isEqualTo(winnerId);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("draw.read"));
    }

    private DrawResult result(UUID drawId, String status) {
        Instant now = Instant.parse("2026-07-07T08:00:00Z");
        UUID winnerId = UUID.randomUUID();
        return new DrawResult(
                drawId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "AUCTION",
                status,
                now.plusSeconds(3600),
                winnerId,
                List.of(new AuctionBidResult(UUID.randomUUID(), winnerId, 10_000L, "INR", now.plusSeconds(3700), "LEADING")),
                now,
                now,
                0);
    }

    private DrawResult resultWithoutWinner() {
        Instant now = Instant.parse("2026-07-07T08:00:00Z");
        return new DrawResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "RANDOM",
                "SCHEDULED",
                now.plusSeconds(3600),
                null,
                List.of(),
                now,
                now,
                0);
    }

    private DrawSummary summary() {
        return new DrawSummary(
                UUID.randomUUID(), 1, "RANDOM", "SCHEDULED", Instant.parse("2026-07-07T08:00:00Z"), null);
    }
}
