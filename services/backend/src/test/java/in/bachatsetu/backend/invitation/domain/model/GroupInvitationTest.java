package in.bachatsetu.backend.invitation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.invitation.domain.event.InvitationAccepted;
import in.bachatsetu.backend.invitation.domain.event.InvitationCreated;
import in.bachatsetu.backend.invitation.domain.event.InvitationRevoked;
import in.bachatsetu.backend.invitation.domain.exception.InvitationDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GroupInvitationTest {

    private static final Instant NOW = Instant.parse("2026-07-09T06:00:00Z");
    private static final InvitationCode CODE = new InvitationCode("AB3D9F2K");
    private static final InvitationToken TOKEN = new InvitationToken("a".repeat(43));

    @Test
    void createsAsActiveAndEmitsInvitationCreated() {
        AggregateId groupId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        GroupInvitation invitation = createInvitation(groupId, actorId);

        assertThat(invitation.status()).isEqualTo(InvitationStatus.ACTIVE);
        assertThat(invitation.domainEvents()).singleElement().isInstanceOf(InvitationCreated.class);
    }

    @Test
    void revokesAnActiveInvitationExactlyOnce() {
        AggregateId groupId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        GroupInvitation invitation = createInvitation(groupId, actorId);

        invitation.revoke(actorId, NOW.plusSeconds(1));

        assertThat(invitation.status()).isEqualTo(InvitationStatus.CANCELLED);
        assertThat(invitation.domainEvents()).anyMatch(InvitationRevoked.class::isInstance);
        assertThatThrownBy(() -> invitation.revoke(actorId, NOW.plusSeconds(2)))
                .isInstanceOf(InvitationDomainException.class);
    }

    @Test
    void acceptsAnActiveInvitationExactlyOnce() {
        AggregateId groupId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        AggregateId joiner = AggregateId.newId();
        GroupInvitation invitation = createInvitation(groupId, actorId);

        invitation.accept(joiner, InvitationType.CODE, NOW.plusSeconds(1));

        assertThat(invitation.status()).isEqualTo(InvitationStatus.USED);
        assertThat(invitation.acceptedBy()).isEqualTo(joiner);
        assertThat(invitation.acceptedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(invitation.domainEvents()).anyMatch(InvitationAccepted.class::isInstance);
        InvitationAccepted event = (InvitationAccepted) invitation.domainEvents().getLast();
        assertThat(event.channel()).isEqualTo(InvitationType.CODE);

        assertThatThrownBy(() -> invitation.accept(joiner, InvitationType.CODE, NOW.plusSeconds(2)))
                .isInstanceOf(InvitationDomainException.class);
    }

    @Test
    void rejectsAcceptanceOfAnExpiredInvitation() {
        AggregateId groupId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        GroupInvitation invitation = GroupInvitation.create(
                AggregateId.newId(), AggregateId.newId(), groupId, CODE, TOKEN, InvitationType.QR,
                NOW.plusSeconds(60), actorId, NOW);

        assertThatThrownBy(() -> invitation.accept(AggregateId.newId(), InvitationType.QR, NOW.plusSeconds(120)))
                .isInstanceOf(InvitationDomainException.class);
    }

    @Test
    void rejectsAcceptanceOfARevokedInvitation() {
        AggregateId groupId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        GroupInvitation invitation = createInvitation(groupId, actorId);
        invitation.revoke(actorId, NOW.plusSeconds(1));

        assertThatThrownBy(() -> invitation.accept(AggregateId.newId(), InvitationType.LINK, NOW.plusSeconds(2)))
                .isInstanceOf(InvitationDomainException.class);
    }

    private GroupInvitation createInvitation(AggregateId groupId, AggregateId actorId) {
        return GroupInvitation.create(
                AggregateId.newId(), AggregateId.newId(), groupId, CODE, TOKEN, InvitationType.CODE,
                NOW.plusSeconds(604_800), actorId, NOW);
    }
}
