package in.bachatsetu.backend.support.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
import in.bachatsetu.backend.support.domain.port.SupportTicketRepository;
import org.junit.jupiter.api.Test;

class SupportApplicationConfigTest {

    private final SupportApplicationConfig config = new SupportApplicationConfig();
    private final SupportApplicationMapper mapper = new SupportApplicationMapper();

    @Test
    void composesCreateTicketUseCase() {
        assertThat(config.createTicketUseCase(
                        mock(SupportTicketRepository.class), mock(DomainEventPublisherPort.class),
                        mock(ClockPort.class), mock(TransactionPort.class), mapper))
                .isInstanceOf(CreateTicketApplicationService.class);
    }

    @Test
    void composesGetTicketUseCase() {
        assertThat(config.getTicketUseCase(mock(SupportTicketRepository.class), mock(TransactionPort.class), mapper))
                .isInstanceOf(GetTicketApplicationService.class);
    }

    @Test
    void composesSearchTicketsUseCase() {
        assertThat(config.searchTicketsUseCase(mock(SupportTicketRepository.class), mock(TransactionPort.class), mapper))
                .isInstanceOf(SearchTicketsApplicationService.class);
    }

    @Test
    void composesAssignTicketUseCase() {
        assertThat(config.assignTicketUseCase(
                        mock(SupportTicketRepository.class), mock(DomainEventPublisherPort.class),
                        mock(ClockPort.class), mock(TransactionPort.class), mapper))
                .isInstanceOf(AssignTicketApplicationService.class);
    }

    @Test
    void composesResolveTicketUseCase() {
        assertThat(config.resolveTicketUseCase(
                        mock(SupportTicketRepository.class), mock(DomainEventPublisherPort.class),
                        mock(ClockPort.class), mock(TransactionPort.class), mapper))
                .isInstanceOf(ResolveTicketApplicationService.class);
    }

    @Test
    void composesCloseTicketUseCase() {
        assertThat(config.closeTicketUseCase(
                        mock(SupportTicketRepository.class), mock(DomainEventPublisherPort.class),
                        mock(ClockPort.class), mock(TransactionPort.class), mapper))
                .isInstanceOf(CloseTicketApplicationService.class);
    }
}
