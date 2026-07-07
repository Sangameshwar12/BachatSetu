package in.bachatsetu.backend.receipt.interfaces.rest.config;

import in.bachatsetu.backend.receipt.application.mapper.ReceiptApplicationMapper;
import in.bachatsetu.backend.receipt.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.receipt.application.port.ReceiptPdfGenerator;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
import in.bachatsetu.backend.receipt.application.service.CreateReceiptApplicationService;
import in.bachatsetu.backend.receipt.application.service.GetReceiptApplicationService;
import in.bachatsetu.backend.receipt.application.service.GetReceiptPdfApplicationService;
import in.bachatsetu.backend.receipt.application.service.ListReceiptsApplicationService;
import in.bachatsetu.backend.receipt.application.usecase.CreateReceiptUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptUseCase;
import in.bachatsetu.backend.receipt.application.usecase.ListReceiptsUseCase;
import in.bachatsetu.backend.receipt.domain.factory.ReceiptFactory;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composes framework-free Receipt application services when all outbound ports exist. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({
    ReceiptRepository.class,
    ReceiptFactory.class,
    DomainEventPublisherPort.class,
    TransactionPort.class,
    ReceiptPdfGenerator.class
})
public class ReceiptApplicationConfig {

    @Bean
    public ReceiptApplicationMapper receiptApplicationMapper() {
        return new ReceiptApplicationMapper();
    }

    @Bean
    public CreateReceiptUseCase createReceiptUseCase(
            ReceiptRepository repository,
            ReceiptFactory receiptFactory,
            DomainEventPublisherPort eventPublisher,
            TransactionPort transaction,
            ReceiptApplicationMapper mapper) {
        return new CreateReceiptApplicationService(repository, receiptFactory, eventPublisher, transaction, mapper);
    }

    @Bean
    public GetReceiptUseCase getReceiptUseCase(
            ReceiptRepository repository,
            TransactionPort transaction,
            ReceiptApplicationMapper mapper) {
        return new GetReceiptApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ListReceiptsUseCase listReceiptsUseCase(
            ReceiptRepository repository,
            TransactionPort transaction,
            ReceiptApplicationMapper mapper) {
        return new ListReceiptsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public GetReceiptPdfUseCase getReceiptPdfUseCase(
            GetReceiptUseCase getReceiptUseCase,
            ReceiptPdfGenerator pdfGenerator) {
        return new GetReceiptPdfApplicationService(getReceiptUseCase, pdfGenerator);
    }
}
