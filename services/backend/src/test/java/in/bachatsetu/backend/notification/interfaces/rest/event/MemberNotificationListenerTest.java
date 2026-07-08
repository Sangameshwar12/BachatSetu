package in.bachatsetu.backend.notification.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.member.domain.event.MemberStatusChanged;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.model.MemberStatus;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Email;
import in.bachatsetu.backend.user.domain.event.UserContactChanged;
import in.bachatsetu.backend.user.domain.model.UserContact;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MemberNotificationListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private MemberRepository memberRepository;
    private CreateNotificationUseCase createNotification;
    private MemberNotificationListener listener;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        createNotification = mock(CreateNotificationUseCase.class);
        listener = new MemberNotificationListener(memberRepository, createNotification);
    }

    @Test
    void notifiesTheMemberWhenTheirStatusChanges() {
        MemberProfile member = newMember();
        member.changeStatus(MemberStatus.ACTIVE, member.userId(), NOW.plusSeconds(60));
        when(memberRepository.findById(member.id())).thenReturn(Optional.of(member));

        listener.onMemberStatusChanged(new MemberStatusChanged(
                UUID.randomUUID(), member.id(), MemberStatus.INVITED, MemberStatus.ACTIVE, NOW));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification).execute(captor.capture());
        CreateNotificationCommand command = captor.getValue();
        assertThat(command.tenantId()).isEqualTo(member.tenantId());
        assertThat(command.recipientUserId()).isEqualTo(member.userId());
        assertThat(command.category()).isEqualTo(NotificationCategory.MEMBER);
        assertThat(command.placeholders()).containsEntry("title", "Status Changed");
    }

    @Test
    void doesNothingWhenTheMemberCannotBeFoundForAStatusChange() {
        AggregateId memberId = AggregateId.newId();
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        listener.onMemberStatusChanged(new MemberStatusChanged(
                UUID.randomUUID(), memberId, MemberStatus.INVITED, MemberStatus.ACTIVE, NOW));

        verify(createNotification, never()).execute(any());
    }

    @Test
    void notifiesTheUserWhenTheirProfileContactChanges() {
        MemberProfile member = newMember();
        when(memberRepository.findByUserId(member.userId())).thenReturn(Optional.of(member));
        UserContact previous = new UserContact(new Email("old@example.com"), null);
        UserContact current = new UserContact(new Email("new@example.com"), null);

        listener.onUserContactChanged(new UserContactChanged(
                UUID.randomUUID(), member.userId(), previous, current, NOW));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification).execute(captor.capture());
        CreateNotificationCommand command = captor.getValue();
        assertThat(command.tenantId()).isEqualTo(member.tenantId());
        assertThat(command.recipientUserId()).isEqualTo(member.userId());
        assertThat(command.category()).isEqualTo(NotificationCategory.MEMBER);
        assertThat(command.placeholders()).containsEntry("title", "Profile Updated");
    }

    @Test
    void doesNothingWhenTheUserHasNoMemberProfile() {
        AggregateId userId = AggregateId.newId();
        when(memberRepository.findByUserId(userId)).thenReturn(Optional.empty());
        UserContact contact = new UserContact(new Email("solo@example.com"), null);

        listener.onUserContactChanged(new UserContactChanged(UUID.randomUUID(), userId, contact, contact, NOW));

        verify(createNotification, never()).execute(any());
    }

    @Test
    void swallowsAndLogsANotificationFailureWithoutRethrowing() {
        MemberProfile member = newMember();
        when(memberRepository.findById(member.id())).thenReturn(Optional.of(member));
        when(createNotification.execute(any())).thenThrow(new RuntimeException("dispatch failed"));

        assertThatCode(() -> listener.onMemberStatusChanged(new MemberStatusChanged(
                        UUID.randomUUID(), member.id(), MemberStatus.INVITED, MemberStatus.ACTIVE, NOW)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullConstructorArguments() {
        assertThatThrownBy(() -> new MemberNotificationListener(null, createNotification))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MemberNotificationListener(memberRepository, null))
                .isInstanceOf(NullPointerException.class);
    }

    private MemberProfile newMember() {
        return MemberProfile.create(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new MemberNumber("MB-TESTMEMBER001"), AggregateId.newId(), NOW);
    }
}
