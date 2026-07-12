package in.bachatsetu.backend.member.application.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.member.application.exception.MemberAccessDeniedException;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemberAuthorizationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-07T08:00:00Z");

    private final MemberAuthorizationService authorization = new MemberAuthorizationService();

    @Test
    void allowsTheMemberThemselves() {
        MemberProfile member = newMember();

        authorization.requireSelf(member, member.userId());
    }

    @Test
    void deniesAnyOtherActor() {
        MemberProfile member = newMember();
        AggregateId otherActor = AggregateId.newId();

        assertThatThrownBy(() -> authorization.requireSelf(member, otherActor))
                .isInstanceOf(MemberAccessDeniedException.class)
                .hasMessage("only the member themselves may perform this operation");
    }

    @Test
    void rejectsNullInputs() {
        MemberProfile member = newMember();

        assertThatThrownBy(() -> authorization.requireSelf((MemberProfile) null, member.userId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> authorization.requireSelf(member, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void allowsTargetUserIdMatchingActor() {
        AggregateId userId = AggregateId.newId();

        authorization.requireSelf(userId, userId);
    }

    @Test
    void deniesTargetUserIdNotMatchingActor() {
        AggregateId targetUserId = AggregateId.newId();
        AggregateId otherActor = AggregateId.newId();

        assertThatThrownBy(() -> authorization.requireSelf(targetUserId, otherActor))
                .isInstanceOf(MemberAccessDeniedException.class)
                .hasMessage("only the member themselves may perform this operation");
    }

    @Test
    void rejectsNullInputsForTargetUserIdOverload() {
        AggregateId userId = AggregateId.newId();

        assertThatThrownBy(() -> authorization.requireSelf((AggregateId) null, userId))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> authorization.requireSelf(userId, (AggregateId) null))
                .isInstanceOf(NullPointerException.class);
    }

    private MemberProfile newMember() {
        return MemberProfile.create(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new MemberNumber("MB-1A2B3C4D5E6F7A8B"), AggregateId.newId(), NOW);
    }
}
