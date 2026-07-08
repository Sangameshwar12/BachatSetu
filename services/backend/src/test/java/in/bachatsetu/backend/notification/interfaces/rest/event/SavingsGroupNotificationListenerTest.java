package in.bachatsetu.backend.notification.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.event.MemberJoined;
import in.bachatsetu.backend.group.domain.event.MemberRemoved;
import in.bachatsetu.backend.group.domain.event.SavingsGroupCreated;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SavingsGroupNotificationListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private GroupRepository groupRepository;
    private CreateNotificationUseCase createNotification;
    private SavingsGroupNotificationListener listener;

    @BeforeEach
    void setUp() {
        groupRepository = mock(GroupRepository.class);
        createNotification = mock(CreateNotificationUseCase.class);
        listener = new SavingsGroupNotificationListener(groupRepository, createNotification);
    }

    @Test
    void notifiesTheOwnerWhenAGroupIsCreated() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId ownerId = AggregateId.newId();

        listener.onGroupCreated(new SavingsGroupCreated(
                UUID.randomUUID(), AggregateId.newId(), tenantId, new OwnerId(ownerId),
                new GroupCode("BST-0001"), NOW));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification).execute(captor.capture());
        CreateNotificationCommand command = captor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.recipientUserId()).isEqualTo(ownerId);
        assertThat(command.category()).isEqualTo(NotificationCategory.GROUP);
        assertThat(command.placeholders()).containsEntry("title", "Group Created");
    }

    @Test
    void notifiesTheJoiningMemberWhenTheyJoinAGroup() {
        AggregateId ownerId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        SavingsGroup group = GroupDomainFixtures.newGroup(ownerId, 5);
        when(groupRepository.findById(group.id())).thenReturn(Optional.of(group));

        listener.onMemberJoined(new MemberJoined(UUID.randomUUID(), group.id(), memberId, NOW));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification).execute(captor.capture());
        CreateNotificationCommand command = captor.getValue();
        assertThat(command.tenantId()).isEqualTo(group.tenantId());
        assertThat(command.recipientUserId()).isEqualTo(memberId);
        assertThat(command.category()).isEqualTo(NotificationCategory.GROUP);
        assertThat(command.placeholders()).containsEntry("title", "Member Joined");
    }

    @Test
    void notifiesTheRemovedMemberWhenTheyAreRemoved() {
        AggregateId ownerId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        SavingsGroup group = GroupDomainFixtures.newGroup(ownerId, 5);
        when(groupRepository.findById(group.id())).thenReturn(Optional.of(group));

        listener.onMemberRemoved(new MemberRemoved(UUID.randomUUID(), group.id(), memberId, NOW));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification).execute(captor.capture());
        assertThat(captor.getValue().placeholders()).containsEntry("title", "Member Removed");
    }

    @Test
    void doesNothingWhenTheGroupCannotBeFoundForMembershipEvents() {
        AggregateId groupId = AggregateId.newId();
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        listener.onMemberJoined(new MemberJoined(UUID.randomUUID(), groupId, AggregateId.newId(), NOW));
        listener.onMemberRemoved(new MemberRemoved(UUID.randomUUID(), groupId, AggregateId.newId(), NOW));

        verify(createNotification, never()).execute(any());
    }

    @Test
    void swallowsAndLogsANotificationFailureWithoutRethrowing() {
        when(createNotification.execute(any())).thenThrow(new RuntimeException("dispatch failed"));

        assertThatCode(() -> listener.onGroupCreated(new SavingsGroupCreated(
                        UUID.randomUUID(), AggregateId.newId(), AggregateId.newId(),
                        new OwnerId(AggregateId.newId()), new GroupCode("BST-0002"), NOW)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullConstructorArguments() {
        assertThatThrownBy(() -> new SavingsGroupNotificationListener(null, createNotification))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SavingsGroupNotificationListener(groupRepository, null))
                .isInstanceOf(NullPointerException.class);
    }
}
