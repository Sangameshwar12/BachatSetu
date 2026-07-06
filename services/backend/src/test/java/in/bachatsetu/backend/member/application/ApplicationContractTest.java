package in.bachatsetu.backend.member.application;

import static in.bachatsetu.backend.member.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.member.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.member.application.command.CreateMemberProfileCommand;
import in.bachatsetu.backend.member.application.command.JoinGroupParticipationCommand;
import in.bachatsetu.backend.member.application.exception.DuplicateMemberNumberException;
import in.bachatsetu.backend.member.application.exception.MemberApplicationException;
import in.bachatsetu.backend.member.application.exception.MemberProfileNotFoundException;
import in.bachatsetu.backend.member.application.port.ClockPort;
import in.bachatsetu.backend.member.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.member.application.port.MemberNumberGeneratorPort;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApplicationContractTest {

    private static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    @Test
    void commandContractsCarryRequiredContext() {
        CreateMemberProfileCommand create = createCommand();
        AggregateId tenantId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        AggregateId groupId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();

        assertThat(new JoinGroupParticipationCommand(tenantId, memberId, groupId, GroupRole.MEMBER, actorId).memberId())
                .isEqualTo(memberId);
        assertThat(create.role()).isEqualTo(GroupRole.MEMBER);
    }

    @Test
    void commandContractsRejectNullContext() {
        CreateMemberProfileCommand create = createCommand();
        AggregateId id = AggregateId.newId();

        assertThatThrownBy(() -> new CreateMemberProfileCommand(
                        null, create.userId(), create.groupId(), create.role(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateMemberProfileCommand(
                        create.tenantId(), null, create.groupId(), create.role(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateMemberProfileCommand(
                        create.tenantId(), create.userId(), null, create.role(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateMemberProfileCommand(
                        create.tenantId(), create.userId(), create.groupId(), null, create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateMemberProfileCommand(
                        create.tenantId(), create.userId(), create.groupId(), create.role(), null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new JoinGroupParticipationCommand(null, id, id, GroupRole.MEMBER, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationCommand(id, null, id, GroupRole.MEMBER, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationCommand(id, id, null, GroupRole.MEMBER, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationCommand(id, id, id, null, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupParticipationCommand(id, id, id, GroupRole.MEMBER, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void outboundPortsExposeRequiredOperations() {
        ClockPort clock = () -> NOW;
        AtomicReference<List<?>> published = new AtomicReference<>();
        DomainEventPublisherPort publisher = published::set;
        TransactionPort transaction = directTransaction();
        MemberNumberGeneratorPort generator = ignored -> new MemberNumber("MB-PORTNUMBER0001");

        assertThat(clock.now()).isEqualTo(NOW);
        publisher.publish(List.of());
        assertThat(published.get()).isEmpty();
        assertThat(transaction.execute(() -> "committed")).isEqualTo("committed");
        assertThat(generator.generate(AggregateId.newId())).isEqualTo(new MemberNumber("MB-PORTNUMBER0001"));
    }

    @Test
    void useCaseAndExceptionContractsArePresent() {
        List<Class<?>> useCases = List.of(
                in.bachatsetu.backend.member.application.usecase.CreateMemberProfileUseCase.class,
                in.bachatsetu.backend.member.application.usecase.JoinGroupParticipationUseCase.class,
                in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase.class);

        assertThat(useCases).allMatch(Class::isInterface);
        assertThat(new MemberApplicationException("application failure")).hasMessage("application failure");
        assertThat(new MemberProfileNotFoundException("missing")).isInstanceOf(MemberApplicationException.class);
        assertThat(new DuplicateMemberNumberException("duplicate")).isInstanceOf(MemberApplicationException.class);
    }

    @Test
    void domainPortExposesTenantScopedLookup() {
        Set<String> methods = Arrays.stream(in.bachatsetu.backend.member.domain.port.MemberRepository.class
                        .getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methods).contains("findById", "findByUserId", "findByMemberNumber", "save");
    }
}
