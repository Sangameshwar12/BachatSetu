package in.bachatsetu.backend.audit.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.audit.application.mapper.AuditApplicationMapper;
import in.bachatsetu.backend.audit.application.port.AuditPublisherPort;
import in.bachatsetu.backend.audit.application.port.ClockPort;
import in.bachatsetu.backend.audit.application.port.TransactionPort;
import in.bachatsetu.backend.audit.application.service.CreateAuditEntryApplicationService;
import in.bachatsetu.backend.audit.application.service.GetAuditEntryApplicationService;
import in.bachatsetu.backend.audit.application.service.SearchAuditApplicationService;
import in.bachatsetu.backend.audit.domain.port.AuditRepository;
import org.junit.jupiter.api.Test;

class AuditApplicationConfigTest {

    private final AuditApplicationConfig config = new AuditApplicationConfig();
    private final AuditApplicationMapper mapper = config.auditApplicationMapper();
    private final AuditRepository repository = mock(AuditRepository.class);
    private final AuditPublisherPort publisher = mock(AuditPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);

    @Test
    void composesCreateAuditEntryUseCase() {
        assertThat(config.createAuditEntryUseCase(repository, publisher, clock, transaction, mapper))
                .isInstanceOf(CreateAuditEntryApplicationService.class);
    }

    @Test
    void composesSearchAuditUseCase() {
        assertThat(config.searchAuditUseCase(repository, transaction, mapper))
                .isInstanceOf(SearchAuditApplicationService.class);
    }

    @Test
    void composesGetAuditEntryUseCase() {
        assertThat(config.getAuditEntryUseCase(repository, transaction, mapper))
                .isInstanceOf(GetAuditEntryApplicationService.class);
    }
}
