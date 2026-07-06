package in.bachatsetu.backend.member.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Safe application view of one recorded member consent. */
public record MemberConsentResult(
        UUID consentId,
        String type,
        String documentVersion,
        Instant grantedAt) {

    public MemberConsentResult {
        Objects.requireNonNull(consentId, "consent id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(documentVersion, "document version must not be null");
        Objects.requireNonNull(grantedAt, "granted at must not be null");
    }
}
