package in.bachatsetu.backend.receipt.interfaces.rest.config;

import in.bachatsetu.backend.receipt.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.receipt.application.port.ReceiptPdfGenerator;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
import in.bachatsetu.backend.receipt.domain.factory.ReceiptFactory;
import in.bachatsetu.backend.receipt.interfaces.rest.adapter.ApplicationEventReceiptEventPublisherAdapter;
import in.bachatsetu.backend.receipt.interfaces.rest.adapter.OpenPdfReceiptPdfGenerator;
import in.bachatsetu.backend.receipt.interfaces.rest.adapter.SpringReceiptTransactionAdapter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Composes the Receipt outbound port adapters backing the application layer. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(PlatformTransactionManager.class)
public class ReceiptInfrastructureConfig {

    @Bean
    Clock receiptClock() {
        return Clock.systemUTC();
    }

    @Bean
    ReceiptFactory receiptFactory(Clock receiptClock) {
        return new ReceiptFactory(receiptClock);
    }

    @Bean
    TransactionTemplate receiptTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    TransactionPort springReceiptTransactionAdapter(TransactionTemplate receiptTransactionTemplate) {
        return new SpringReceiptTransactionAdapter(receiptTransactionTemplate);
    }

    @Bean
    DomainEventPublisherPort applicationEventReceiptEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventReceiptEventPublisherAdapter(publisher);
    }

    @Bean
    ReceiptPdfGenerator openPdfReceiptPdfGenerator() {
        return new OpenPdfReceiptPdfGenerator();
    }
}
