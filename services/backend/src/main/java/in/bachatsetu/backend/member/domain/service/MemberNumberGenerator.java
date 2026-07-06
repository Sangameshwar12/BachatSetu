package in.bachatsetu.backend.member.domain.service;

import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Locale;
import java.util.Objects;

/** Generates a stable human-facing member number from an already unique aggregate identifier. */
public final class MemberNumberGenerator {

    private static final int CODE_ID_LENGTH = 16;

    public MemberNumber generate(AggregateId memberId) {
        Objects.requireNonNull(memberId, "member id must not be null");
        String compactId = memberId.value().toString().replace("-", "");
        return new MemberNumber("MB-" + compactId.substring(0, CODE_ID_LENGTH).toUpperCase(Locale.ROOT));
    }
}
