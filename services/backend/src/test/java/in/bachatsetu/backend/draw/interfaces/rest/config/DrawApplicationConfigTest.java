package in.bachatsetu.backend.draw.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.draw.application.mapper.DrawApplicationMapper;
import in.bachatsetu.backend.draw.application.port.ClockPort;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.application.security.DrawAuthorizationService;
import in.bachatsetu.backend.draw.application.service.CloseDrawApplicationService;
import in.bachatsetu.backend.draw.application.service.ConductDrawApplicationService;
import in.bachatsetu.backend.draw.application.service.CreateDrawApplicationService;
import in.bachatsetu.backend.draw.application.service.GetDrawApplicationService;
import in.bachatsetu.backend.draw.application.service.ListDrawsApplicationService;
import in.bachatsetu.backend.draw.domain.factory.DrawFactory;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class DrawApplicationConfigTest {

    private final DrawApplicationConfig config = new DrawApplicationConfig();
    private final DrawApplicationMapper mapper = config.drawApplicationMapper();
    private final DrawRepository repository = mock(DrawRepository.class);
    private final SavingsGroupRepository groupRepository = mock(SavingsGroupRepository.class);
    private final DrawFactory drawFactory = new DrawFactory(Clock.systemUTC());
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);
    private final DrawAuthorizationService authorization = config.drawAuthorizationService();

    @Test
    void composesDrawAuthorizationService() {
        assertThat(config.drawAuthorizationService()).isInstanceOf(DrawAuthorizationService.class);
    }

    @Test
    void composesCreateDrawUseCase() {
        assertThat(config.createDrawUseCase(
                        repository, groupRepository, drawFactory, eventPublisher, transaction, mapper, authorization))
                .isInstanceOf(CreateDrawApplicationService.class);
    }

    @Test
    void composesGetDrawUseCase() {
        assertThat(config.getDrawUseCase(repository, transaction, mapper))
                .isInstanceOf(GetDrawApplicationService.class);
    }

    @Test
    void composesListDrawsUseCase() {
        assertThat(config.listDrawsUseCase(repository, transaction, mapper))
                .isInstanceOf(ListDrawsApplicationService.class);
    }

    @Test
    void composesConductDrawUseCase() {
        assertThat(config.conductDrawUseCase(
                        repository, groupRepository, eventPublisher, clock, transaction, mapper, authorization))
                .isInstanceOf(ConductDrawApplicationService.class);
    }

    @Test
    void composesCloseDrawUseCase() {
        assertThat(config.closeDrawUseCase(
                        repository, groupRepository, eventPublisher, clock, transaction, mapper, authorization))
                .isInstanceOf(CloseDrawApplicationService.class);
    }
}
