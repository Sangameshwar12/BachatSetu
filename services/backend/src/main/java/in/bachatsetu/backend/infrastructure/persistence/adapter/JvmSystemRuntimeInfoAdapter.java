package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.platformoperations.domain.model.SystemRuntimeInfo;
import in.bachatsetu.backend.platformoperations.domain.port.SystemRuntimeInfoPort;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reads JVM and host facts directly — no actuator redesign, this composes with the existing
 * {@code /actuator/health} endpoint rather than replacing it. {@code buildTimestamp} is {@code null}: this
 * project has no {@code spring-boot-maven-plugin} {@code build-info} execution configured, so there is no
 * real build timestamp to report (a known limitation, not a placeholder).
 */
@Component
@ConditionalOnPersistenceRepositories
public class JvmSystemRuntimeInfoAdapter implements SystemRuntimeInfoPort {

    private final String applicationVersion;

    public JvmSystemRuntimeInfoAdapter(@Value("${info.application.version:unknown}") String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @Override
    public SystemRuntimeInfo current() {
        Runtime runtime = Runtime.getRuntime();
        File root = new File(".");
        long uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
        return new SystemRuntimeInfo(
                uptimeSeconds, System.getProperty("java.version"), applicationVersion, null,
                runtime.totalMemory() - runtime.freeMemory(), runtime.totalMemory(), runtime.maxMemory(),
                root.getUsableSpace(), root.getTotalSpace());
    }
}
