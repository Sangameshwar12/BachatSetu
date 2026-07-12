package in.bachatsetu.backend.member.application.service;

import static in.bachatsetu.backend.member.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.member.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.member.application.command.CreateMemberProfileCommand;
import in.bachatsetu.backend.member.application.command.JoinGroupParticipationCommand;
import in.bachatsetu.backend.member.application.command.UpdateMemberProfileCommand;
import in.bachatsetu.backend.member.application.exception.DuplicateMemberNumberException;
import in.bachatsetu.backend.member.application.exception.MemberProfileNotFoundException;
import in.bachatsetu.backend.member.application.mapper.MemberApplicationMapper;
import in.bachatsetu.backend.member.application.port.ClockPort;
import in.bachatsetu.backend.member.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.member.application.port.MemberNumberGeneratorPort;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.application.exception.MemberAccessDeniedException;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.query.MemberProfileSummary;
import in.bachatsetu.backend.member.application.security.MemberAuthorizationService;
import in.bachatsetu.backend.member.application.usecase.CreateMemberProfileUseCase;
import in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase;
import in.bachatsetu.backend.member.application.usecase.JoinGroupParticipationUseCase;
import in.bachatsetu.backend.member.application.usecase.ListMemberProfilesUseCase;
import in.bachatsetu.backend.member.application.usecase.UpdateMemberProfileUseCase;
import in.bachatsetu.backend.member.domain.event.MemberCreated;
import in.bachatsetu.backend.member.domain.event.MemberJoinedGroup;
import in.bachatsetu.backend.member.domain.event.MemberStatusChanged;
import in.bachatsetu.backend.member.domain.exception.DuplicateGroupParticipationException;
import in.bachatsetu.backend.member.domain.exception.InvalidMembershipStateException;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.model.MemberStatus;
import in.bachatsetu.backend.member.domain.port.MemberPage;
import in.bachatsetu.backend.member.domain.port.MemberPageRequest;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.member.domain.port.MemberSortField;
import in.bachatsetu.backend.member.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MemberApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    private MemberRepository repository;
    private MemberNumberGeneratorPort numberGenerator;
    private DomainEventPublisherPort publisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private MemberApplicationMapper mapper;
    private MemberAuthorizationService authorization;

    @BeforeEach
    void setUp() {
        repository = mock(MemberRepository.class);
        numberGenerator = mock(MemberNumberGeneratorPort.class);
        publisher = mock(DomainEventPublisherPort.class);
        clock = () -> NOW.plusSeconds(10);
        transaction = directTransaction();
        mapper = new MemberApplicationMapper();
        authorization = new MemberAuthorizationService();
    }

    @Test
    void createsSavesPublishesAndMapsMemberProfile() {
        CreateMemberProfileCommand command = createCommand();
        when(numberGenerator.generate(any())).thenReturn(new MemberNumber("MB-APP-CREATE0001"));
        when(repository.findByMemberNumber(command.tenantId(), new MemberNumber("MB-APP-CREATE0001")))
                .thenReturn(Optional.empty());
        CreateMemberProfileUseCase service = new CreateMemberProfileApplicationService(
                repository, numberGenerator, publisher, clock, transaction, mapper, authorization);

        MemberProfileResult result = service.execute(command);

        assertThat(result.memberNumber()).isEqualTo("MB-APP-CREATE0001");
        assertThat(result.status()).isEqualTo("INVITED");
        assertThat(result.participations()).singleElement().satisfies(participation -> {
            assertThat(participation.groupId()).isEqualTo(command.groupId().value());
            assertThat(participation.role()).isEqualTo("MEMBER");
        });
        verify(repository).save(any(MemberProfile.class));
        assertPublishedEvents(MemberCreated.class, MemberJoinedGroup.class);
    }

    @Test
    void rejectsDuplicateOrNullGeneratedNumber() {
        CreateMemberProfileCommand command = createCommand();
        when(numberGenerator.generate(any())).thenReturn(new MemberNumber("MB-APP-DUPLICATE1"));
        when(repository.findByMemberNumber(command.tenantId(), new MemberNumber("MB-APP-DUPLICATE1")))
                .thenReturn(Optional.of(mock(MemberProfile.class)));
        CreateMemberProfileApplicationService duplicateService = new CreateMemberProfileApplicationService(
                repository, numberGenerator, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> duplicateService.execute(command))
                .isInstanceOf(DuplicateMemberNumberException.class);
        verify(repository, never()).save(any());

        when(numberGenerator.generate(any())).thenReturn(null);
        CreateMemberProfileApplicationService nullNumberService = new CreateMemberProfileApplicationService(
                repository, numberGenerator, publisher, clock, transaction, mapper, authorization);
        assertThatThrownBy(() -> nullNumberService.execute(createCommand())).isInstanceOf(NullPointerException.class);
    }

    @Test
    void doesNotPublishWhenPersistenceFails() {
        CreateMemberProfileCommand command = createCommand();
        when(numberGenerator.generate(any())).thenReturn(new MemberNumber("MB-APP-SAVEFAIL1"));
        AtomicReference<MemberProfile> attempted = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
                    attempted.set(invocation.getArgument(0));
                    throw new IllegalStateException("storage unavailable");
                })
                .when(repository)
                .save(any(MemberProfile.class));
        CreateMemberProfileApplicationService service = new CreateMemberProfileApplicationService(
                repository, numberGenerator, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(IllegalStateException.class);

        verify(publisher, never()).publish(any());
        assertThat(attempted.get().domainEvents()).hasSize(2)
                .anyMatch(MemberCreated.class::isInstance)
                .anyMatch(MemberJoinedGroup.class::isInstance);
    }

    @Test
    void deniesCreatingAProfileForAnotherUser() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId targetUserId = AggregateId.newId();
        AggregateId otherActorId = AggregateId.newId();
        CreateMemberProfileCommand command = new CreateMemberProfileCommand(
                tenantId, targetUserId, AggregateId.newId(), GroupRole.MEMBER, otherActorId);
        CreateMemberProfileUseCase service = new CreateMemberProfileApplicationService(
                repository, numberGenerator, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(MemberAccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void joinsAdditionalGroupThroughExistingProfile() {
        AggregateId tenantId = AggregateId.newId();
        MemberProfile member = existingMember(tenantId);
        AggregateId newGroupId = AggregateId.newId();
        when(repository.findById(tenantId, member.id())).thenReturn(Optional.of(member));
        JoinGroupParticipationUseCase service = new JoinGroupParticipationApplicationService(
                repository, publisher, clock, transaction, mapper, authorization);

        MemberProfileResult result = service.execute(
                new JoinGroupParticipationCommand(tenantId, member.id(), newGroupId, GroupRole.MEMBER, member.userId()));

        assertThat(result.participations()).hasSize(2);
        verify(repository).save(member);
        assertPublishedEvents(MemberJoinedGroup.class);
    }

    @Test
    void rejectsDuplicateParticipationOnJoin() {
        AggregateId tenantId = AggregateId.newId();
        MemberProfile member = existingMember(tenantId);
        AggregateId existingGroupId = member.participations().get(0).groupId();
        when(repository.findById(tenantId, member.id())).thenReturn(Optional.of(member));
        JoinGroupParticipationUseCase service = new JoinGroupParticipationApplicationService(
                repository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(new JoinGroupParticipationCommand(
                        tenantId, member.id(), existingGroupId, GroupRole.MEMBER, member.userId())))
                .isInstanceOf(DuplicateGroupParticipationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deniesJoiningAnAdditionalGroupOnAnothersProfile() {
        AggregateId tenantId = AggregateId.newId();
        MemberProfile member = existingMember(tenantId);
        AggregateId otherActorId = AggregateId.newId();
        when(repository.findById(tenantId, member.id())).thenReturn(Optional.of(member));
        JoinGroupParticipationUseCase service = new JoinGroupParticipationApplicationService(
                repository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(new JoinGroupParticipationCommand(
                        tenantId, member.id(), AggregateId.newId(), GroupRole.MEMBER, otherActorId)))
                .isInstanceOf(MemberAccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void reportsMissingMemberOnJoinWithoutSavingOrPublishing() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        when(repository.findById(tenantId, memberId)).thenReturn(Optional.empty());
        JoinGroupParticipationUseCase service = new JoinGroupParticipationApplicationService(
                repository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(new JoinGroupParticipationCommand(
                        tenantId, memberId, AggregateId.newId(), GroupRole.MEMBER, AggregateId.newId())))
                .isInstanceOf(MemberProfileNotFoundException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void retrievesOwnMemberProfile() {
        AggregateId tenantId = AggregateId.newId();
        MemberProfile member = existingMember(tenantId);
        when(repository.findById(tenantId, member.id())).thenReturn(Optional.of(member));
        GetMemberProfileUseCase service = new GetMemberProfileApplicationService(
                repository, transaction, mapper, authorization);

        MemberProfileResult result = service.execute(tenantId, member.id(), member.userId());

        assertThat(result.memberId()).isEqualTo(member.id().value());
    }

    @Test
    void deniesViewingAnotherMembersProfile() {
        AggregateId tenantId = AggregateId.newId();
        MemberProfile member = existingMember(tenantId);
        AggregateId otherUserId = AggregateId.newId();
        when(repository.findById(tenantId, member.id())).thenReturn(Optional.of(member));
        GetMemberProfileUseCase service = new GetMemberProfileApplicationService(
                repository, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(tenantId, member.id(), otherUserId))
                .isInstanceOf(MemberAccessDeniedException.class);
    }

    @Test
    void tenantScopedLookupHidesMembersFromOtherTenants() {
        AggregateId tenantId = AggregateId.newId();
        MemberProfile member = existingMember(AggregateId.newId());
        when(repository.findById(tenantId, member.id())).thenReturn(Optional.empty());
        GetMemberProfileUseCase service = new GetMemberProfileApplicationService(
                repository, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(tenantId, member.id(), member.userId()))
                .isInstanceOf(MemberProfileNotFoundException.class);
    }

    @Test
    void listsTenantScopedMemberSummaries() {
        AggregateId tenantId = AggregateId.newId();
        MemberProfile first = existingMember(tenantId);
        MemberProfile second = existingMember(tenantId);
        MemberPageRequest pageRequest = new MemberPageRequest(0, 20, MemberSortField.CREATED_AT, SortDirection.ASC);
        when(repository.findPage(tenantId, pageRequest))
                .thenReturn(new MemberPage<>(List.of(first, second), 0, 20, 2));
        ListMemberProfilesUseCase service = new ListMemberProfilesApplicationService(repository, transaction, mapper);

        MemberPage<MemberProfileSummary> page = service.execute(tenantId, pageRequest);

        assertThat(page.content()).hasSize(2).extracting(MemberProfileSummary::participationCount)
                .containsExactly(1, 1);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThatThrownBy(() -> page.content().add(mapper.toSummary(first)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void updatesMemberStatusAndPublishesEvent() {
        AggregateId tenantId = AggregateId.newId();
        MemberProfile member = existingMember(tenantId);
        when(repository.findById(tenantId, member.id())).thenReturn(Optional.of(member));
        UpdateMemberProfileUseCase service = new UpdateMemberProfileApplicationService(
                repository, publisher, clock, transaction, mapper, authorization);

        MemberProfileResult result = service.execute(
                new UpdateMemberProfileCommand(tenantId, member.id(), MemberStatus.ACTIVE, member.userId()));

        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(repository).save(member);
        assertPublishedEvents(MemberStatusChanged.class);
    }

    @Test
    void deniesUpdatingAnotherMembersStatus() {
        AggregateId tenantId = AggregateId.newId();
        MemberProfile member = existingMember(tenantId);
        AggregateId otherUserId = AggregateId.newId();
        when(repository.findById(tenantId, member.id())).thenReturn(Optional.of(member));
        UpdateMemberProfileUseCase service = new UpdateMemberProfileApplicationService(
                repository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(
                        new UpdateMemberProfileCommand(tenantId, member.id(), MemberStatus.ACTIVE, otherUserId)))
                .isInstanceOf(MemberAccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rejectsInvalidStatusTransitionWithoutSavingOrPublishing() {
        AggregateId tenantId = AggregateId.newId();
        MemberProfile member = existingMember(tenantId);
        when(repository.findById(tenantId, member.id())).thenReturn(Optional.of(member));
        UpdateMemberProfileUseCase service = new UpdateMemberProfileApplicationService(
                repository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(
                        new UpdateMemberProfileCommand(tenantId, member.id(), MemberStatus.INVITED, member.userId())))
                .isInstanceOf(InvalidMembershipStateException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void reportsMissingMemberOnUpdateWithoutSavingOrPublishing() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        when(repository.findById(tenantId, memberId)).thenReturn(Optional.empty());
        UpdateMemberProfileUseCase service = new UpdateMemberProfileApplicationService(
                repository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(
                        new UpdateMemberProfileCommand(tenantId, memberId, MemberStatus.ACTIVE, AggregateId.newId())))
                .isInstanceOf(MemberProfileNotFoundException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rejectsNullUseCaseInputs() {
        assertThatThrownBy(() -> new CreateMemberProfileApplicationService(
                        repository, numberGenerator, publisher, clock, transaction, mapper, authorization)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationApplicationService(
                        repository, publisher, clock, transaction, mapper, authorization)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetMemberProfileApplicationService(repository, transaction, mapper, authorization)
                        .execute(null, AggregateId.newId(), AggregateId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetMemberProfileApplicationService(repository, transaction, mapper, authorization)
                        .execute(AggregateId.newId(), null, AggregateId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetMemberProfileApplicationService(repository, transaction, mapper, authorization)
                        .execute(AggregateId.newId(), AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListMemberProfilesApplicationService(repository, transaction, mapper)
                        .execute(null, new MemberPageRequest(0, 20, MemberSortField.CREATED_AT, SortDirection.ASC)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListMemberProfilesApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateMemberProfileApplicationService(
                        repository, publisher, clock, transaction, mapper, authorization)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new CreateMemberProfileApplicationService(
                        null, numberGenerator, publisher, clock, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateMemberProfileApplicationService(
                        repository, null, publisher, clock, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateMemberProfileApplicationService(
                        repository, numberGenerator, publisher, clock, transaction, mapper, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationApplicationService(
                        null, publisher, clock, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationApplicationService(
                        repository, null, clock, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationApplicationService(
                        repository, publisher, null, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationApplicationService(
                        repository, publisher, clock, null, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationApplicationService(
                        repository, publisher, clock, transaction, null, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationApplicationService(
                        repository, publisher, clock, transaction, mapper, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetMemberProfileApplicationService(null, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetMemberProfileApplicationService(repository, null, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetMemberProfileApplicationService(repository, transaction, null, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetMemberProfileApplicationService(repository, transaction, mapper, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListMemberProfilesApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListMemberProfilesApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListMemberProfilesApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateMemberProfileApplicationService(
                        null, publisher, clock, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateMemberProfileApplicationService(
                        repository, null, clock, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateMemberProfileApplicationService(
                        repository, publisher, null, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateMemberProfileApplicationService(
                        repository, publisher, clock, null, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateMemberProfileApplicationService(
                        repository, publisher, clock, transaction, null, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateMemberProfileApplicationService(
                        repository, publisher, clock, transaction, mapper, null))
                .isInstanceOf(NullPointerException.class);
    }

    private MemberProfile existingMember(AggregateId tenantId) {
        MemberProfile member = MemberProfile.create(
                AggregateId.newId(), tenantId, AggregateId.newId(),
                new MemberNumber("MB-EXISTING000001"), AggregateId.newId(), NOW);
        member.joinGroup(AggregateId.newId(), GroupRole.MEMBER, member.userId(), NOW.plusSeconds(1));
        member.pullDomainEvents();
        return member;
    }

    @SafeVarargs
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertPublishedEvents(Class<? extends DomainEvent>... eventTypes) {
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue()).hasSize(eventTypes.length);
        for (Class<? extends DomainEvent> eventType : eventTypes) {
            assertThat(captor.getValue()).anyMatch(eventType::isInstance);
        }
    }
}
