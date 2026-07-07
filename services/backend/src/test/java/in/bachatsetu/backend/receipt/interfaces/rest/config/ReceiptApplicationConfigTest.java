package in.bachatsetu.backend.receipt.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.receipt.application.mapper.ReceiptApplicationMapper;
import in.bachatsetu.backend.receipt.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
import in.bachatsetu.backend.receipt.application.service.CreateReceiptApplicationService;
import in.bachatsetu.backend.receipt.application.service.GetReceiptApplicationService;
import in.bachatsetu.backend.receipt.application.service.ListReceiptsApplicationService;
import in.bachatsetu.backend.receipt.domain.factory.ReceiptFactory;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class ReceiptApplicationConfigTest {

    private final ReceiptApplicationConfig config = new ReceiptApplicationConfig();
    private final ReceiptApplicationMapper mapper = config.receiptApplicationMapper();
    private final ReceiptRepository repository = mock(ReceiptRepository.class);
    private final ReceiptFactory receiptFactory = new ReceiptFactory(Clock.systemUTC());
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);

    @Test
    void composesCreateReceiptUseCase() {
        assertThat(config.createReceiptUseCase(repository, receiptFactory, eventPublisher, transaction, mapper))
                .isInstanceOf(CreateReceiptApplicationService.class);
    }

    @Test
    void composesGetReceiptUseCase() {
        assertThat(config.getReceiptUseCase(repository, transaction, mapper))
                .isInstanceOf(GetReceiptApplicationService.class);
    }

    @Test
    void composesListReceiptsUseCase() {
        assertThat(config.listReceiptsUseCase(repository, transaction, mapper))
                .isInstanceOf(ListReceiptsApplicationService.class);
    }
}
