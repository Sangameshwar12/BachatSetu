package in.bachatsetu.backend.support.interfaces.rest.config;

import in.bachatsetu.backend.support.application.mapper.SupportApplicationMapper;
import in.bachatsetu.backend.support.application.port.ClockPort;
import in.bachatsetu.backend.support.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.support.application.port.TransactionPort;
import in.bachatsetu.backend.support.application.service.AssignTicketApplicationService;
import in.bachatsetu.backend.support.application.service.CloseTicketApplicationService;
import in.bachatsetu.backend.support.application.service.CreateTicketApplicationService;
import in.bachatsetu.backend.support.application.service.GetTicketApplicationService;
import in.bachatsetu.backend.support.application.service.ResolveTicketApplicationService;
import in.bachatsetu.backend.support.application.service.SearchTicketsApplicationService;
import in.bachatsetu.backend.support.application.usecase.AssignTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.CloseTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.CreateTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.GetTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.ResolveTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.SearchTicketsUseCase;
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Support application services.
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
public class SupportApplicationConfig {

    @Bean
    public SupportApplicationMapper supportApplicationMapper() {
        return new SupportApplicationMapper();
    }

    @Bean
    public CreateTicketUseCase createTicketUseCase(
            SupportTicketRepository repository, DomainEventPublisherPort eventPublisher, ClockPort clock,
            TransactionPort transaction, SupportApplicationMapper mapper) {
        return new CreateTicketApplicationService(repository, eventPublisher, clock, transaction, mapper);
    }

    @Bean
    public GetTicketUseCase getTicketUseCase(
            SupportTicketRepository repository, TransactionPort transaction, SupportApplicationMapper mapper) {
        return new GetTicketApplicationService(repository, transaction, mapper);
    }

    @Bean
    public SearchTicketsUseCase searchTicketsUseCase(
            SupportTicketRepository repository, TransactionPort transaction, SupportApplicationMapper mapper) {
        return new SearchTicketsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public AssignTicketUseCase assignTicketUseCase(
            SupportTicketRepository repository, DomainEventPublisherPort eventPublisher, ClockPort clock,
            TransactionPort transaction, SupportApplicationMapper mapper) {
        return new AssignTicketApplicationService(repository, eventPublisher, clock, transaction, mapper);
    }

    @Bean
    public ResolveTicketUseCase resolveTicketUseCase(
            SupportTicketRepository repository, DomainEventPublisherPort eventPublisher, ClockPort clock,
            TransactionPort transaction, SupportApplicationMapper mapper) {
        return new ResolveTicketApplicationService(repository, eventPublisher, clock, transaction, mapper);
    }

    @Bean
    public CloseTicketUseCase closeTicketUseCase(
            SupportTicketRepository repository, DomainEventPublisherPort eventPublisher, ClockPort clock,
            TransactionPort transaction, SupportApplicationMapper mapper) {
        return new CloseTicketApplicationService(repository, eventPublisher, clock, transaction, mapper);
    }
}
