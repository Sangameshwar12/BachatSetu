package in.bachatsetu.backend.auth.domain.port;

import in.bachatsetu.backend.auth.domain.model.Role;
import in.bachatsetu.backend.auth.domain.model.RoleId;
import java.util.Optional;

/** Persistence port for authentication roles. */
public interface RoleRepository {

    Optional<Role> findById(RoleId roleId);

    Optional<Role> findByName(String name);

    void save(Role role);
}
