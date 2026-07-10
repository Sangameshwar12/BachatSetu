package in.bachatsetu.backend.platformoperations.domain.port;

import in.bachatsetu.backend.platformoperations.domain.model.SystemRuntimeInfo;

@FunctionalInterface
public interface SystemRuntimeInfoPort {

    SystemRuntimeInfo current();
}
