package in.bachatsetu.backend.member.application.mapper;

import in.bachatsetu.backend.member.application.query.GroupParticipationResult;
import in.bachatsetu.backend.member.application.query.MemberConsentResult;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.query.MemberProfileSummary;
import in.bachatsetu.backend.member.domain.model.GroupParticipation;
import in.bachatsetu.backend.member.domain.model.MemberConsent;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import java.util.Objects;

/** Maps the Member domain aggregate to immutable application query models. */
public final class MemberApplicationMapper {

    public MemberProfileResult toResult(MemberProfile member) {
        Objects.requireNonNull(member, "member profile must not be null");
        return new MemberProfileResult(
                member.id().value(),
                member.tenantId().value(),
                member.userId().value(),
                member.memberNumber().value(),
                member.status().name(),
                member.participations().stream().map(this::toParticipationResult).toList(),
                member.consents().stream().map(this::toConsentResult).toList(),
                member.version());
    }

    public MemberProfileSummary toSummary(MemberProfile member) {
        Objects.requireNonNull(member, "member profile must not be null");
        return new MemberProfileSummary(
                member.id().value(),
                member.userId().value(),
                member.memberNumber().value(),
                member.status().name(),
                member.participations().size());
    }

    public GroupParticipationResult toParticipationResult(GroupParticipation participation) {
        Objects.requireNonNull(participation, "group participation must not be null");
        return new GroupParticipationResult(
                participation.groupId().value(),
                participation.role().name(),
                participation.joinedAt(),
                participation.exitedAt(),
                participation.status().name());
    }

    public MemberConsentResult toConsentResult(MemberConsent consent) {
        Objects.requireNonNull(consent, "member consent must not be null");
        return new MemberConsentResult(
                consent.id().value(), consent.type().name(), consent.documentVersion(), consent.grantedAt());
    }
}
