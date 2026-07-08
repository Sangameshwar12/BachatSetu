package in.bachatsetu.backend.audit.interfaces.rest.config;

import in.bachatsetu.backend.audit.application.mapper.AuditApplicationMapper;
import in.bachatsetu.backend.audit.application.port.AuditPublisherPort;
import in.bachatsetu.backend.audit.application.port.ClockPort;
import in.bachatsetu.backend.audit.application.port.TransactionPort;
import in.bachatsetu.backend.audit.application.service.CreateAuditEntryApplicationService;
import in.bachatsetu.backend.audit.application.service.GetAuditEntryApplicationService;
import in.bachatsetu.backend.audit.application.service.SearchAuditApplicationService;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.application.usecase.GetAuditEntryUseCase;
import in.bachatsetu.backend.audit.application.usecase.SearchAuditUseCase;
import in.bachatsetu.backend.audit.domain.port.AuditRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Audit application services.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's application
 * config.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuditApplicationConfig {

    @Bean
    public AuditApplicationMapper auditApplicationMapper() {
        return new AuditApplicationMapper();
    }

    @Bean
    public CreateAuditEntryUseCase createAuditEntryUseCase(
            AuditRepository repository,
            AuditPublisherPort publisher,
            ClockPort clock,
            TransactionPort transaction,
            AuditApplicationMapper mapper) {
        return new CreateAuditEntryApplicationService(repository, publisher, clock, transaction, mapper);
    }

    @Bean
    public SearchAuditUseCase searchAuditUseCase(
            AuditRepository repository, TransactionPort transaction, AuditApplicationMapper mapper) {
        return new SearchAuditApplicationService(repository, transaction, mapper);
    }

    @Bean
    public GetAuditEntryUseCase getAuditEntryUseCase(
            AuditRepository repository, TransactionPort transaction, AuditApplicationMapper mapper) {
        return new GetAuditEntryApplicationService(repository, transaction, mapper);
    }
}
