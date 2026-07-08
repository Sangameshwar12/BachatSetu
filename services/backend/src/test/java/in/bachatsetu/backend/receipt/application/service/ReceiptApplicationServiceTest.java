package in.bachatsetu.backend.receipt.application.service;

import static in.bachatsetu.backend.receipt.application.ApplicationTestFixture.NOW;
import static in.bachatsetu.backend.receipt.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.receipt.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.receipt.application.command.CreateReceiptCommand;
import in.bachatsetu.backend.receipt.application.exception.ReceiptNotFoundException;
import in.bachatsetu.backend.receipt.application.mapper.ReceiptApplicationMapper;
import in.bachatsetu.backend.receipt.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.receipt.application.port.ReceiptPdfGenerator;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
import in.bachatsetu.backend.receipt.application.query.ReceiptPdfResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptSummary;
import in.bachatsetu.backend.receipt.application.usecase.CreateReceiptUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptUseCase;
import in.bachatsetu.backend.receipt.application.usecase.ListReceiptsUseCase;
import in.bachatsetu.backend.receipt.domain.event.ReceiptGenerated;
import in.bachatsetu.backend.receipt.domain.factory.ReceiptFactory;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptDescription;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.receipt.domain.model.ReceiptType;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPage;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPageRequest;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import in.bachatsetu.backend.receipt.domain.port.ReceiptSortField;
import in.bachatsetu.backend.receipt.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReceiptApplicationServiceTest {

    private static final Pattern NUMBER_FORMAT = Pattern.compile("^RCT/\\d{8}/[0-9A-F]{8}$");

    private ReceiptRepository repository;
    private ReceiptFactory receiptFactory;
    private DomainEventPublisherPort publisher;
    private TransactionPort transaction;
    private ReceiptApplicationMapper mapper;
    private CreateAuditEntryUseCase createAuditEntry;

    @BeforeEach
    void setUp() {
        repository = mock(ReceiptRepository.class);
        receiptFactory = new ReceiptFactory(Clock.fixed(NOW, ZoneOffset.UTC));
        publisher = mock(DomainEventPublisherPort.class);
        transaction = directTransaction();
        mapper = new ReceiptApplicationMapper();
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
    }

    @Test
    void createsSavesPublishesAndMapsReceipt() {
        CreateReceiptCommand command = createCommand();
        when(repository.findByPaymentId(command.paymentId())).thenReturn(Optional.empty());
        CreateReceiptUseCase service =
                new CreateReceiptApplicationService(repository, receiptFactory, publisher, transaction, mapper, createAuditEntry);

        ReceiptResult result = service.execute(command);

        assertThat(result.status()).isEqualTo("GENERATED");
        assertThat(result.number()).matches(NUMBER_FORMAT);
        assertThat(result.totalAmountPaise()).isEqualTo(500_000L);
        verify(repository).save(any(Receipt.class));
        assertPublishedEvents(ReceiptGenerated.class);
        verify(createAuditEntry).execute(any());
    }

    @Test
    void generatesUniqueReceiptNumbersAcrossCalls() {
        when(repository.findByPaymentId(any())).thenReturn(Optional.empty());
        CreateReceiptUseCase service =
                new CreateReceiptApplicationService(repository, receiptFactory, publisher, transaction, mapper, createAuditEntry);

        ReceiptResult first = service.execute(createCommand());
        ReceiptResult second = service.execute(createCommand());

        assertThat(first.number()).matches(NUMBER_FORMAT);
        assertThat(second.number()).matches(NUMBER_FORMAT);
        assertThat(first.number()).isNotEqualTo(second.number());
    }

    @Test
    void createReturnsTheExistingReceiptForARepeatedPayment() {
        CreateReceiptCommand command = createCommand();
        Receipt existing = newReceipt(command.paymentId());
        when(repository.findByPaymentId(command.paymentId())).thenReturn(Optional.of(existing));
        CreateReceiptUseCase service =
                new CreateReceiptApplicationService(repository, receiptFactory, publisher, transaction, mapper, createAuditEntry);

        ReceiptResult result = service.execute(command);

        assertThat(result.receiptId()).isEqualTo(existing.id().value());
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void retrievesTenantScopedReceipt() {
        AggregateId tenantId = AggregateId.newId();
        Receipt receipt = newReceipt(AggregateId.newId());
        when(repository.findById(tenantId, receipt.id())).thenReturn(Optional.of(receipt));
        GetReceiptUseCase service = new GetReceiptApplicationService(repository, transaction, mapper);

        ReceiptResult result = service.execute(tenantId, receipt.id());

        assertThat(result.receiptId()).isEqualTo(receipt.id().value());
    }

    @Test
    void tenantScopedLookupHidesReceiptsFromOtherTenants() {
        AggregateId tenantId = AggregateId.newId();
        Receipt receipt = newReceipt(AggregateId.newId());
        when(repository.findById(tenantId, receipt.id())).thenReturn(Optional.empty());
        GetReceiptUseCase service = new GetReceiptApplicationService(repository, transaction, mapper);

        assertThatThrownBy(() -> service.execute(tenantId, receipt.id()))
                .isInstanceOf(ReceiptNotFoundException.class);
    }

    @Test
    void listsTenantScopedReceiptSummaries() {
        AggregateId tenantId = AggregateId.newId();
        Receipt first = newReceipt(AggregateId.newId());
        Receipt second = newReceipt(AggregateId.newId());
        ReceiptPageRequest pageRequest = new ReceiptPageRequest(0, 20, ReceiptSortField.CREATED_AT, SortDirection.ASC);
        when(repository.findPage(tenantId, pageRequest))
                .thenReturn(new ReceiptPage<>(List.of(first, second), 0, 20, 2));
        ListReceiptsUseCase service = new ListReceiptsApplicationService(repository, transaction, mapper);

        ReceiptPage<ReceiptSummary> page = service.execute(tenantId, pageRequest);

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThatThrownBy(() -> page.content().add(mapper.toSummary(first)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void pdfServiceLoadsReceiptAndReturnsGeneratedBytes() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId receiptId = AggregateId.newId();
        ReceiptResult receiptResult = mapper.toResult(newReceipt(AggregateId.newId()));
        GetReceiptUseCase getReceipt = mock(GetReceiptUseCase.class);
        ReceiptPdfGenerator pdfGenerator = mock(ReceiptPdfGenerator.class);
        byte[] pdfBytes = {1, 2, 3, 4};
        when(getReceipt.execute(tenantId, receiptId)).thenReturn(receiptResult);
        when(pdfGenerator.generate(receiptResult)).thenReturn(pdfBytes);
        GetReceiptPdfUseCase service = new GetReceiptPdfApplicationService(getReceipt, pdfGenerator, createAuditEntry);

        ReceiptPdfResult result = service.execute(tenantId, receiptId);

        assertThat(result.content()).containsExactly(pdfBytes);
        assertThat(result.fileName()).isEqualTo(receiptResult.number().replace("/", "-") + ".pdf");
        verify(createAuditEntry).execute(any());
    }

    @Test
    void pdfServicePropagatesNotFoundForCrossTenantOrMissingReceipts() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId receiptId = AggregateId.newId();
        GetReceiptUseCase getReceipt = mock(GetReceiptUseCase.class);
        ReceiptPdfGenerator pdfGenerator = mock(ReceiptPdfGenerator.class);
        when(getReceipt.execute(tenantId, receiptId))
                .thenThrow(new ReceiptNotFoundException("receipt does not exist"));
        GetReceiptPdfUseCase service = new GetReceiptPdfApplicationService(getReceipt, pdfGenerator, createAuditEntry);

        assertThatThrownBy(() -> service.execute(tenantId, receiptId))
                .isInstanceOf(ReceiptNotFoundException.class);
    }

    @Test
    void pdfServiceRejectsNullInputs() {
        GetReceiptUseCase getReceipt = mock(GetReceiptUseCase.class);
        ReceiptPdfGenerator pdfGenerator = mock(ReceiptPdfGenerator.class);
        GetReceiptPdfUseCase service = new GetReceiptPdfApplicationService(getReceipt, pdfGenerator, createAuditEntry);

        assertThatThrownBy(() -> service.execute(null, AggregateId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetReceiptPdfApplicationService(null, pdfGenerator, createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetReceiptPdfApplicationService(getReceipt, null, createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetReceiptPdfApplicationService(getReceipt, pdfGenerator, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullUseCaseInputs() {
        assertThatThrownBy(() -> new CreateReceiptApplicationService(
                        repository, receiptFactory, publisher, transaction, mapper, createAuditEntry)
                        .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetReceiptApplicationService(repository, transaction, mapper)
                        .execute(null, AggregateId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetReceiptApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListReceiptsApplicationService(repository, transaction, mapper)
                        .execute(null, new ReceiptPageRequest(0, 20, ReceiptSortField.CREATED_AT, SortDirection.ASC)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListReceiptsApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new CreateReceiptApplicationService(
                        null, receiptFactory, publisher, transaction, mapper, createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateReceiptApplicationService(
                        repository, null, publisher, transaction, mapper, createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateReceiptApplicationService(
                        repository, receiptFactory, publisher, null, mapper, createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateReceiptApplicationService(
                        repository, receiptFactory, publisher, transaction, null, createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateReceiptApplicationService(
                        repository, receiptFactory, publisher, transaction, mapper, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetReceiptApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetReceiptApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetReceiptApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListReceiptsApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListReceiptsApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListReceiptsApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }

    private Receipt newReceipt(AggregateId paymentId) {
        List<ReceiptLine> lines = List.of(new ReceiptLine(
                AggregateId.newId(), ReceiptType.CONTRIBUTION,
                new ReceiptDescription("Monthly contribution"), Money.inr(500_000)));
        return Receipt.generate(
                AggregateId.newId(), AggregateId.newId(), paymentId, AggregateId.newId(),
                new ReceiptNumber("RCT/20260707/00000001"), lines, AggregateId.newId(), NOW);
    }

    @SafeVarargs
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertPublishedEvents(Class<? extends DomainEvent>... eventTypes) {
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue()).hasSize(eventTypes.length);
        for (Class<? extends DomainEvent> eventType : eventTypes) {
            assertThat(captor.getValue()).anyMatch(eventType::isInstance);
        }
    }
}
