package in.bachatsetu.backend.receipt.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.receipt.application.command.CreateReceiptCommand;
import in.bachatsetu.backend.receipt.application.query.ReceiptLineResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptPdfResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptSummary;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptUseCase;
import in.bachatsetu.backend.receipt.application.usecase.ListReceiptsUseCase;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPage;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPageRequest;
import in.bachatsetu.backend.receipt.domain.port.ReceiptSortField;
import in.bachatsetu.backend.receipt.domain.port.SortDirection;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.CreateReceiptRequest;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.ReceiptLineRequest;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.ReceiptResponse;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.ReceiptSummaryResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReceiptApiMapperTest {

    private final ReceiptApiMapper mapper = new ReceiptApiMapper();

    @Test
    void mapsCreateRequestToCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID paymentId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        CreateReceiptRequest request = new CreateReceiptRequest(
                paymentId.toString(), memberId.toString(),
                List.of(new ReceiptLineRequest("CONTRIBUTION", "Monthly contribution", 500_000L)));

        CreateReceiptCommand command = mapper.toCreateCommand(request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.paymentId().value()).isEqualTo(paymentId);
        assertThat(command.memberId().value()).isEqualTo(memberId);
        assertThat(command.lines()).singleElement().satisfies(line -> {
            assertThat(line.type().name()).isEqualTo("CONTRIBUTION");
            assertThat(line.description().value()).isEqualTo("Monthly contribution");
            assertThat(line.amount().minorUnits()).isEqualTo(500_000L);
        });
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void getReceiptDelegatesToUseCaseWithParsedIdentifiers() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID receiptId = UUID.randomUUID();
        ReceiptResult expected = result(receiptId, "GENERATED");
        GetReceiptUseCase useCase = (tenantId, id) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(id.value()).isEqualTo(receiptId);
            return expected;
        };

        assertThat(mapper.getReceipt(useCase, currentUser, receiptId.toString())).isEqualTo(expected);
    }

    @Test
    void getReceiptPdfDelegatesToUseCaseWithParsedIdentifiers() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID receiptId = UUID.randomUUID();
        ReceiptPdfResult expected = new ReceiptPdfResult(new byte[] {1, 2, 3}, "RCT-20260807-1A2B3C4D.pdf");
        GetReceiptPdfUseCase useCase = (tenantId, id) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(id.value()).isEqualTo(receiptId);
            return expected;
        };

        ReceiptPdfResult result = mapper.getReceiptPdf(useCase, currentUser, receiptId.toString());

        assertThat(result.fileName()).isEqualTo(expected.fileName());
        assertThat(result.content()).containsExactly(expected.content());
    }

    @Test
    void mapsResultToResponseIncludingLines() {
        UUID receiptId = UUID.randomUUID();
        ReceiptResult result = result(receiptId, "GENERATED");

        ReceiptResponse response = mapper.toResponse(result);

        assertThat(response.receiptId()).isEqualTo(receiptId.toString());
        assertThat(response.status()).isEqualTo("GENERATED");
        assertThat(response.lines()).singleElement()
                .satisfies(line -> assertThat(line.type()).isEqualTo("CONTRIBUTION"));
    }

    @Test
    void buildsPageRequestFromValidatedRestParameters() {
        ReceiptPageRequest pageRequest = mapper.toPageRequest(1, 10, "amount", "desc");

        assertThat(pageRequest.page()).isEqualTo(1);
        assertThat(pageRequest.size()).isEqualTo(10);
        assertThat(pageRequest.sortField()).isEqualTo(ReceiptSortField.AMOUNT);
        assertThat(pageRequest.direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void rejectsUnsupportedSortOrDirectionValues() {
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "unsupported", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "amount", "sideways"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listReceiptsDelegatesToUseCaseWithTenantIdentityAndPageRequest() {
        AuthenticatedUser currentUser = authenticatedUser();
        ReceiptPageRequest pageRequest =
                new ReceiptPageRequest(0, 20, ReceiptSortField.CREATED_AT, SortDirection.ASC);
        ReceiptPage<ReceiptSummary> expected = new ReceiptPage<>(List.of(summary()), 0, 20, 1);
        ListReceiptsUseCase useCase = (tenantId, request) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(request).isEqualTo(pageRequest);
            return expected;
        };

        assertThat(mapper.listReceipts(useCase, currentUser, pageRequest)).isEqualTo(expected);
    }

    @Test
    void listReceiptsConsolidatesPageRequestAndResponseForTheController() {
        AuthenticatedUser currentUser = authenticatedUser();
        ListReceiptsUseCase useCase = (tenantId, request) -> new ReceiptPage<>(List.of(summary()), 0, 20, 1);

        PageResponse<ReceiptSummaryResponse> response =
                mapper.listReceipts(useCase, currentUser, 0, 20, "createdAt", "asc");

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void mapsSummaryToResponse() {
        ReceiptSummaryResponse response = mapper.toSummaryResponse(summary());

        assertThat(response.status()).isEqualTo("GENERATED");
    }

    @Test
    void mapsReceiptPageToPageResponse() {
        ReceiptPage<ReceiptSummary> page = new ReceiptPage<>(List.of(summary(), summary()), 0, 2, 3);

        PageResponse<ReceiptSummaryResponse> response = mapper.toSummaryPage(page);

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
                Set.of("receipt.read"));
    }

    private ReceiptResult result(UUID receiptId, String status) {
        Instant now = Instant.parse("2026-07-07T08:00:00Z");
        return new ReceiptResult(
                receiptId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "RCT/20260707/1A2B3C4D",
                List.of(new ReceiptLineResult(UUID.randomUUID(), "CONTRIBUTION", "Monthly contribution", 500_000L, "INR")),
                500_000L,
                "INR",
                status,
                null,
                now,
                now,
                0);
    }

    private ReceiptSummary summary() {
        return new ReceiptSummary(
                UUID.randomUUID(), "RCT/20260707/1A2B3C4D", 500_000L, "INR", "GENERATED",
                Instant.parse("2026-07-07T08:00:00Z"));
    }
}
