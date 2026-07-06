package in.bachatsetu.backend.member.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemberApplicationMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    private final MemberApplicationMapper mapper = new MemberApplicationMapper();

    @Test
    void mapsMemberProfileToResultIncludingParticipations() {
        AggregateId memberId = AggregateId.newId();
        AggregateId tenantId = AggregateId.newId();
        AggregateId userId = AggregateId.newId();
        AggregateId groupId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        MemberProfile member = MemberProfile.create(
                memberId, tenantId, userId, new MemberNumber("MB-1A2B3C4D5E6F7A8B"), actorId, NOW);
        member.joinGroup(groupId, GroupRole.MEMBER, actorId, NOW.plusSeconds(1));

        var result = mapper.toResult(member);

        assertThat(result.memberId()).isEqualTo(memberId.value());
        assertThat(result.tenantId()).isEqualTo(tenantId.value());
        assertThat(result.userId()).isEqualTo(userId.value());
        assertThat(result.memberNumber()).isEqualTo("MB-1A2B3C4D5E6F7A8B");
        assertThat(result.status()).isEqualTo("INVITED");
        assertThat(result.participations()).singleElement().satisfies(participation -> {
            assertThat(participation.groupId()).isEqualTo(groupId.value());
            assertThat(participation.role()).isEqualTo("MEMBER");
            assertThat(participation.status()).isEqualTo("INVITED");
        });
        assertThat(result.consents()).isEmpty();
        assertThatThrownBy(() -> result.participations().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullInputs() {
        assertThatThrownBy(() -> mapper.toResult(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toParticipationResult(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toConsentResult(null)).isInstanceOf(NullPointerException.class);
    }
}
