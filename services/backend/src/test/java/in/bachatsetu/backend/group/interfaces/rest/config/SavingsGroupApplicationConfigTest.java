package in.bachatsetu.backend.group.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.group.application.mapper.SavingsGroupApplicationMapper;
import in.bachatsetu.backend.group.application.port.ClockPort;
import in.bachatsetu.backend.group.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.group.application.port.GroupCodeGeneratorPort;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.application.service.CreateSavingsGroupApplicationService;
import org.junit.jupiter.api.Test;

class SavingsGroupApplicationConfigTest {

    @Test
    void composesCreateSavingsGroupUseCase() {
        SavingsGroupApplicationConfig config = new SavingsGroupApplicationConfig();
        SavingsGroupApplicationMapper mapper = config.savingsGroupApplicationMapper();

        assertThat(config.createSavingsGroupUseCase(
                        mock(SavingsGroupRepository.class),
                        mock(GroupCodeGeneratorPort.class),
                        mock(DomainEventPublisherPort.class),
                        mock(ClockPort.class),
                        mock(TransactionPort.class),
                        mapper))
                .isInstanceOf(CreateSavingsGroupApplicationService.class);
    }
}
