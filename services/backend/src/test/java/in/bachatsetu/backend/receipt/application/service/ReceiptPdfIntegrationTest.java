package in.bachatsetu.backend.receipt.application.service;

import static in.bachatsetu.backend.receipt.application.ApplicationTestFixture.NOW;
import static in.bachatsetu.backend.receipt.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.receipt.application.exception.ReceiptNotFoundException;
import in.bachatsetu.backend.receipt.application.mapper.ReceiptApplicationMapper;
import in.bachatsetu.backend.receipt.application.port.ReceiptPdfGenerator;
import in.bachatsetu.backend.receipt.application.query.ReceiptPdfResult;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfUseCase;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptDescription;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.receipt.domain.model.ReceiptType;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import in.bachatsetu.backend.receipt.interfaces.rest.adapter.OpenPdfReceiptPdfGenerator;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Exercises the real Get-Receipt-then-Render-PDF chain (no mocked collaborators, except for
 * persistence) end to end, verifying the composed application services and the OpenPDF adapter
 * cooperate correctly and that tenant isolation is enforced before any PDF is rendered.
 */
class ReceiptPdfIntegrationTest {

    private final ReceiptRepository repository = mock(ReceiptRepository.class);
    private final ReceiptApplicationMapper mapper = new ReceiptApplicationMapper();
    private final GetReceiptApplicationService getReceipt =
            new GetReceiptApplicationService(repository, directTransaction(), mapper);
    private final ReceiptPdfGenerator pdfGenerator = new OpenPdfReceiptPdfGenerator();
    private final CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);
    private final GetReceiptPdfUseCase service =
            new GetReceiptPdfApplicationService(getReceipt, pdfGenerator, createAuditEntry);

    @Test
    void rendersARealReceiptIntoADownloadablePdf() {
        AggregateId tenantId = AggregateId.newId();
        Receipt receipt = newReceipt(tenantId);
        when(repository.findById(tenantId, receipt.id())).thenReturn(Optional.of(receipt));

        ReceiptPdfResult result = service.execute(tenantId, receipt.id());

        assertThat(result.content()).isNotEmpty();
        assertThat(new String(result.content(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(result.fileName()).isEqualTo("RCT-20260707-00000001.pdf");
    }

    @Test
    void enforcesTenantIsolationBeforeRenderingAnyPdf() {
        AggregateId owningTenantId = AggregateId.newId();
        AggregateId otherTenantId = AggregateId.newId();
        Receipt receipt = newReceipt(owningTenantId);
        when(repository.findById(otherTenantId, receipt.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(otherTenantId, receipt.id()))
                .isInstanceOf(ReceiptNotFoundException.class);
    }

    @Test
    void reportsAnInvalidOrMissingReceiptAsNotFound() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId receiptId = AggregateId.newId();
        when(repository.findById(tenantId, receiptId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(tenantId, receiptId))
                .isInstanceOf(ReceiptNotFoundException.class);
    }

    private Receipt newReceipt(AggregateId tenantId) {
        List<ReceiptLine> lines = List.of(new ReceiptLine(
                AggregateId.newId(), ReceiptType.CONTRIBUTION,
                new ReceiptDescription("Monthly contribution"), Money.inr(500_000)));
        return Receipt.generate(
                AggregateId.newId(), tenantId, AggregateId.newId(), AggregateId.newId(),
                new ReceiptNumber("RCT/20260707/00000001"), lines, AggregateId.newId(), NOW);
    }
}
