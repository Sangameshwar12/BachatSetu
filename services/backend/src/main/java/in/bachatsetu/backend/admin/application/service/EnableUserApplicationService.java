package in.bachatsetu.backend.admin.application.service;

import in.bachatsetu.backend.admin.application.command.EnableUserCommand;
import in.bachatsetu.backend.admin.application.exception.PlatformUserNotFoundException;
import in.bachatsetu.backend.admin.application.mapper.AdminApplicationMapper;
import in.bachatsetu.backend.admin.application.port.ClockPort;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.application.usecase.EnableUserUseCase;
import in.bachatsetu.backend.admin.domain.model.PlatformAdministration;
import in.bachatsetu.backend.admin.domain.model.PlatformUserStatusChange;
import in.bachatsetu.backend.admin.domain.model.PlatformUserSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformUserRepository;
import java.util.Objects;

/**
 * Enables one platform user. Bypasses the Auth module's own {@code User} aggregate and repository entirely
 * — both are always scoped to the caller's own tenant — and instead mutates the shared user record directly
 * through the Admin module's own, cross-tenant {@link PlatformUserRepository}.
 */
public final class EnableUserApplicationService implements EnableUserUseCase {

    private final PlatformUserRepository repository;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final AdminApplicationMapper mapper;

    public EnableUserApplicationService(
            PlatformUserRepository repository, ClockPort clock, TransactionPort transaction,
            AdminApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public PlatformUserResult execute(EnableUserCommand command) {
        Objects.requireNonNull(command, "enable command must not be null");
        return transaction.execute(() -> {
            PlatformAdministration administration = PlatformAdministration.actingAs(command.administratorId());
            PlatformUserStatusChange change = administration.enableUser(command.userId(), clock.now());
            return applyChange(change);
        });
    }

    private PlatformUserResult applyChange(PlatformUserStatusChange change) {
        boolean updated = repository.updateStatus(
                change.userId(), change.targetStatus(), change.administratorId(), change.changedAt());
        if (!updated) {
            throw new PlatformUserNotFoundException("platform user does not exist");
        }
        PlatformUserSummary summary = repository.findById(change.userId())
                .orElseThrow(() -> new PlatformUserNotFoundException("platform user does not exist"));
        return mapper.toResult(summary);
    }
}
