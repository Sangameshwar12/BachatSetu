package in.bachatsetu.backend.admin.application.configuration.service;

import in.bachatsetu.backend.admin.domain.configuration.model.PlatformConfiguration;
import in.bachatsetu.backend.admin.domain.configuration.port.PlatformConfigurationRepository;
import java.time.Instant;
import java.util.Objects;

/**
 * Lightweight, read-only maintenance-mode lookup consulted on the request hot path (the HTTP-level
 * maintenance-mode enforcement filter). Deliberately has no {@code TransactionPort} indirection since the
 * underlying repository adapter is already {@code @Transactional(readOnly = true)} on its own.
 */
public final class MaintenanceStatusQueryService {

    private final PlatformConfigurationRepository repository;

    public MaintenanceStatusQueryService(PlatformConfigurationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public MaintenanceStatus currentStatus(Instant at) {
        Objects.requireNonNull(at, "at must not be null");
        PlatformConfiguration configuration = repository.find();
        return new MaintenanceStatus(configuration.isMaintenanceActiveAt(at), configuration.maintenanceMessage());
    }
}
