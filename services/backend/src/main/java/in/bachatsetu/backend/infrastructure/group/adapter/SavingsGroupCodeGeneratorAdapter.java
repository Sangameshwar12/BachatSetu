package in.bachatsetu.backend.infrastructure.group.adapter;

import in.bachatsetu.backend.group.application.port.GroupCodeGeneratorPort;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.service.GroupCodeGenerator;
import java.util.Objects;

/** Delegates human-facing code generation to the existing domain service. */
public final class SavingsGroupCodeGeneratorAdapter implements GroupCodeGeneratorPort {

    private final GroupCodeGenerator generator;

    public SavingsGroupCodeGeneratorAdapter(GroupCodeGenerator generator) {
        this.generator = Objects.requireNonNull(generator, "generator must not be null");
    }

    @Override
    public GroupCode generate(GroupId groupId) {
        return generator.generate(groupId);
    }
}
