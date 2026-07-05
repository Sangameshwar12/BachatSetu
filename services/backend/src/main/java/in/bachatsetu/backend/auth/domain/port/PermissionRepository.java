package in.bachatsetu.backend.auth.domain.port;

import in.bachatsetu.backend.auth.domain.model.Permission;
import in.bachatsetu.backend.auth.domain.model.PermissionId;
import java.util.Optional;

/** Persistence port for authentication permissions. */
public interface PermissionRepository {

    Optional<Permission> findById(PermissionId permissionId);

    Optional<Permission> findByName(String name);

    void save(Permission permission);
}
