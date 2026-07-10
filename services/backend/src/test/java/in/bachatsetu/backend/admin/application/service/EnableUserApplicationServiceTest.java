package in.bachatsetu.backend.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.application.command.EnableUserCommand;
import in.bachatsetu.backend.admin.application.exception.PlatformUserNotFoundException;
import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformUserSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformUserRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnableUserApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private PlatformUserRepository repository;
    private EnableUserApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(PlatformUserRepository.class);
        ClockPort clock = () -> NOW;
        service = new EnableUserApplicationService(
                repository, clock, new DirectTransactionPort(), new AdminApplicationMapper());
    }

    @Test
    void enablesAnExistingUser() {
        AggregateId userId = AggregateId.newId();
        AggregateId administratorId = AggregateId.newId();
        AggregateId tenantId = AggregateId.newId();
        when(repository.updateStatus(eq(userId), eq(PlatformUserStatus.ACTIVE), eq(administratorId), eq(NOW)))
                .thenReturn(true);
        PlatformUserSummary summary = new PlatformUserSummary(
                userId, tenantId, null, null, null, null, PlatformUserStatus.ACTIVE, NOW);
        when(repository.findById(userId)).thenReturn(Optional.of(summary));

        PlatformUserResult result = service.execute(new EnableUserCommand(userId, administratorId));

        assertThat(result.status()).isEqualTo(PlatformUserStatus.ACTIVE);
        verify(repository).updateStatus(userId, PlatformUserStatus.ACTIVE, administratorId, NOW);
    }

    @Test
    void rejectsAnUnknownUser() {
        AggregateId userId = AggregateId.newId();
        AggregateId administratorId = AggregateId.newId();
        when(repository.updateStatus(any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.execute(new EnableUserCommand(userId, administratorId)))
                .isInstanceOf(PlatformUserNotFoundException.class);
    }

    @Test
    void rejectsANullCommand() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullConstructorDependencies() {
        AdminApplicationMapper mapper = new AdminApplicationMapper();
        TransactionPort transaction = new DirectTransactionPort();
        ClockPort clock = () -> NOW;
        assertThatThrownBy(() -> new EnableUserApplicationService(null, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EnableUserApplicationService(repository, null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EnableUserApplicationService(repository, clock, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EnableUserApplicationService(repository, clock, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static final class DirectTransactionPort implements TransactionPort {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
