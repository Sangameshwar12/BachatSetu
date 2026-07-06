package in.bachatsetu.backend.group.domain.service;

import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupId;
import java.util.Locale;
import java.util.Objects;

/** Generates a stable human-facing code from an already unique aggregate identifier. */
public final class GroupCodeGenerator {

    private static final int CODE_ID_LENGTH = 16;

    public GroupCode generate(GroupId groupId) {
        Objects.requireNonNull(groupId, "group id must not be null");
        String compactId = groupId.value().value().toString().replace("-", "");
        return new GroupCode("BS-" + compactId.substring(0, CODE_ID_LENGTH).toUpperCase(Locale.ROOT));
    }
}
