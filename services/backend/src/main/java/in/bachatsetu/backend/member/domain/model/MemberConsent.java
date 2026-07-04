package in.bachatsetu.backend.member.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public record MemberConsent(
        AggregateId id,
        ConsentType type,
        String documentVersion,
        Instant grantedAt) {

    public MemberConsent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(documentVersion, "documentVersion must not be null");
        documentVersion = documentVersion.strip();
        Objects.requireNonNull(grantedAt, "grantedAt must not be null");
        if (documentVersion.isEmpty() || documentVersion.length() > 40) {
            throw new IllegalArgumentException("documentVersion length is invalid");
        }
    }
}
