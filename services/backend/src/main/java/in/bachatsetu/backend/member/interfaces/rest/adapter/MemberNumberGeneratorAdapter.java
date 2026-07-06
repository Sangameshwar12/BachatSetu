package in.bachatsetu.backend.member.interfaces.rest.adapter;

import in.bachatsetu.backend.member.application.port.MemberNumberGeneratorPort;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.service.MemberNumberGenerator;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Delegates human-facing member number generation to the existing domain service. */
public final class MemberNumberGeneratorAdapter implements MemberNumberGeneratorPort {

    private final MemberNumberGenerator generator;

    public MemberNumberGeneratorAdapter(MemberNumberGenerator generator) {
        this.generator = Objects.requireNonNull(generator, "generator must not be null");
    }

    @Override
    public MemberNumber generate(AggregateId memberId) {
        return generator.generate(memberId);
    }
}
