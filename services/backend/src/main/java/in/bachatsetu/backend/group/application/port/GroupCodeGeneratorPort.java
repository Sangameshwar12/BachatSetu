package in.bachatsetu.backend.group.application.port;

import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupId;

/** Produces a candidate human-facing code for a new aggregate identifier. */
@FunctionalInterface
public interface GroupCodeGeneratorPort {

    GroupCode generate(GroupId groupId);
}
