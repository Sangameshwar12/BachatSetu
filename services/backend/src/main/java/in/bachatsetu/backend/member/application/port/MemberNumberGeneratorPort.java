package in.bachatsetu.backend.member.application.port;

import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Produces a candidate human-facing member number for a new aggregate identifier. */
@FunctionalInterface
public interface MemberNumberGeneratorPort {

    MemberNumber generate(AggregateId memberId);
}
