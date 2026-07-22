package in.bachatsetu.backend.invitation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.application.exception.GroupAccessDeniedException;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.application.command.CreateInvitationCommand;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.invitation.application.exception.InvitationFailureReason;
import in.bachatsetu.backend.invitation.application.port.ClockPort;
import in.bachatsetu.backend.invitation.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.invitation.application.port.InvitationCodeGeneratorPort;
import in.bachatsetu.backend.invitation.application.port.InvitationTokenGeneratorPort;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.application.query.InvitationResult;
import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateInvitationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-21T06:00:00Z");
    private static final AggregateId TENANT_ID = AggregateId.newId();

    private GroupInvitationRepository invitationRepository;
    private SavingsGroupRepository groupRepository;
    private InvitationCodeGeneratorPort codeGenerator;
    private InvitationTokenGeneratorPort tokenGenerator;
    private DomainEventPublisherPort eventPublisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private CreateInvitationApplicationService service;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(GroupInvitationRepository.class);
        groupRepository = mock(SavingsGroupRepository.class);
        codeGenerator = mock(InvitationCodeGeneratorPort.class);
        tokenGenerator = mock(InvitationTokenGeneratorPort.class);
        eventPublisher = mock(DomainEventPublisherPort.class);
        clock = () -> NOW;
        transaction = Supplier::get;
        when(codeGenerator.generate()).thenReturn(new InvitationCode("AB3D9F2K"));
        when(tokenGenerator.generate()).thenReturn(new InvitationToken("a".repeat(43)));
        when(invitationRepository.findActiveByGroup(any(), any())).thenReturn(Optional.empty());
        service = new CreateInvitationApplicationService(
                invitationRepository, groupRepository, codeGenerator, tokenGenerator, eventPublisher, clock,
                transaction, Duration.ofDays(7));
    }

    @Test
    void createsANewInvitationForTheGroupOwner() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));

        InvitationResult result = service.execute(new CreateInvitationCommand(
                TENANT_ID, group.groupId().value(), InvitationType.CODE, group.ownerId().value()));

        assertThat(result.code()).isEqualTo("AB3D9F2K");
        assertThat(result.groupId()).isEqualTo(group.groupId().value());
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        verify(invitationRepository).save(any(GroupInvitation.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void revokesAnyExistingActiveInvitationBeforeCreatingANewOne() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));
        GroupInvitation existing = GroupInvitation.create(
                AggregateId.newId(), TENANT_ID, group.groupId().value(), new InvitationCode("OLDCODE1"),
                new InvitationToken("b".repeat(43)), InvitationType.CODE, NOW.plusSeconds(604_800),
                group.ownerId().value(), NOW.minusSeconds(60));
        when(invitationRepository.findActiveByGroup(TENANT_ID, group.groupId().value()))
                .thenReturn(Optional.of(existing));

        service.execute(new CreateInvitationCommand(
                TENANT_ID, group.groupId().value(), InvitationType.CODE, group.ownerId().value()));

        verify(invitationRepository).save(existing);
        verify(invitationRepository, times(2)).save(any(GroupInvitation.class));
    }

    @Test
    void rejectsANonOwnerActor() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        when(groupRepository.findById(TENANT_ID, group.groupId())).thenReturn(Optional.of(group));
        AggregateId stranger = AggregateId.newId();

        assertThatThrownBy(() -> service.execute(
                        new CreateInvitationCommand(TENANT_ID, group.groupId().value(), InvitationType.CODE, stranger)))
                .isInstanceOf(GroupAccessDeniedException.class);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void rejectsWhenGroupDoesNotExist() {
        AggregateId groupId = AggregateId.newId();
        when(groupRepository.findById(TENANT_ID, new GroupId(groupId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                        new CreateInvitationCommand(TENANT_ID, groupId, InvitationType.CODE, AggregateId.newId())))
                .isInstanceOfSatisfying(InvitationApplicationException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(InvitationFailureReason.GROUP_NOT_FOUND));
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        Duration validity = Duration.ofDays(7);
        assertThatThrownBy(() -> new CreateInvitationApplicationService(
                        null, groupRepository, codeGenerator, tokenGenerator, eventPublisher, clock, transaction, validity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateInvitationApplicationService(
                        invitationRepository, null, codeGenerator, tokenGenerator, eventPublisher, clock, transaction,
                        validity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateInvitationApplicationService(
                        invitationRepository, groupRepository, null, tokenGenerator, eventPublisher, clock, transaction,
                        validity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateInvitationApplicationService(
                        invitationRepository, groupRepository, codeGenerator, null, eventPublisher, clock, transaction,
                        validity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateInvitationApplicationService(
                        invitationRepository, groupRepository, codeGenerator, tokenGenerator, null, clock, transaction,
                        validity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateInvitationApplicationService(
                        invitationRepository, groupRepository, codeGenerator, tokenGenerator, eventPublisher, null,
                        transaction, validity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateInvitationApplicationService(
                        invitationRepository, groupRepository, codeGenerator, tokenGenerator, eventPublisher, clock, null,
                        validity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateInvitationApplicationService(
                        invitationRepository, groupRepository, codeGenerator, tokenGenerator, eventPublisher, clock,
                        transaction, null))
                .isInstanceOf(NullPointerException.class);
    }
}
