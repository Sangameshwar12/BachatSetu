package in.bachatsetu.backend.platformoperations.application.service;

import in.bachatsetu.backend.platformoperations.application.query.SystemHealthResult;
import in.bachatsetu.backend.platformoperations.application.usecase.GetSystemHealthUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.SystemRuntimeInfo;
import in.bachatsetu.backend.platformoperations.domain.port.DatabaseHealthPort;
import in.bachatsetu.backend.platformoperations.domain.port.NotificationHealthPort;
import in.bachatsetu.backend.platformoperations.domain.port.StorageHealthPort;
import in.bachatsetu.backend.platformoperations.domain.port.SystemRuntimeInfoPort;
import java.util.Objects;

public final class GetSystemHealthApplicationService implements GetSystemHealthUseCase {

    private final DatabaseHealthPort databaseHealth;
    private final StorageHealthPort storageHealth;
    private final NotificationHealthPort notificationHealth;
    private final SystemRuntimeInfoPort runtimeInfo;

    public GetSystemHealthApplicationService(
            DatabaseHealthPort databaseHealth,
            StorageHealthPort storageHealth,
            NotificationHealthPort notificationHealth,
            SystemRuntimeInfoPort runtimeInfo) {
        this.databaseHealth = Objects.requireNonNull(databaseHealth, "databaseHealth must not be null");
        this.storageHealth = Objects.requireNonNull(storageHealth, "storageHealth must not be null");
        this.notificationHealth = Objects.requireNonNull(notificationHealth, "notificationHealth must not be null");
        this.runtimeInfo = Objects.requireNonNull(runtimeInfo, "runtimeInfo must not be null");
    }

    @Override
    public SystemHealthResult execute() {
        SystemRuntimeInfo info = runtimeInfo.current();
        return new SystemHealthResult(
                databaseHealth.check(), storageHealth.check(), notificationHealth.check(), info.uptimeSeconds(),
                info.javaVersion(), info.applicationVersion(), info.buildTimestamp(), info.usedMemoryBytes(),
                info.totalMemoryBytes(), info.maxMemoryBytes(), info.usableDiskBytes(), info.totalDiskBytes());
    }
}
