package in.bachatsetu.backend.member.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.member.application.command.CreateMemberProfileCommand;
import in.bachatsetu.backend.member.application.command.JoinGroupParticipationCommand;
import in.bachatsetu.backend.member.application.query.GroupParticipationResult;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase;
import in.bachatsetu.backend.member.interfaces.rest.dto.CreateMemberProfileRequest;
import in.bachatsetu.backend.member.interfaces.rest.dto.JoinGroupParticipationRequest;
import in.bachatsetu.backend.member.interfaces.rest.dto.MemberProfileResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MemberApiMapperTest {

    private final MemberApiMapper mapper = new MemberApiMapper();

    @Test
    void mapsCreateRequestToCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        CreateMemberProfileRequest request = new CreateMemberProfileRequest(
                userId.toString(), groupId.toString(), "MEMBER");

        CreateMemberProfileCommand command = mapper.toCreateCommand(request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.userId().value()).isEqualTo(userId);
        assertThat(command.groupId().value()).isEqualTo(groupId);
        assertThat(command.role().name()).isEqualTo("MEMBER");
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void mapsJoinRequestToCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        JoinGroupParticipationRequest request = new JoinGroupParticipationRequest(groupId.toString(), "CO_ORGANIZER");

        JoinGroupParticipationCommand command = mapper.toJoinCommand(memberId.toString(), request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.memberId().value()).isEqualTo(memberId);
        assertThat(command.groupId().value()).isEqualTo(groupId);
        assertThat(command.role().name()).isEqualTo("CO_ORGANIZER");
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void getMemberDelegatesToUseCaseWithParsedIdentifiers() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID memberId = UUID.randomUUID();
        MemberProfileResult expected = result(memberId);
        GetMemberProfileUseCase useCase = (tenantId, id) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(id.value()).isEqualTo(memberId);
            return expected;
        };

        assertThat(mapper.getMember(useCase, currentUser, memberId.toString())).isEqualTo(expected);
    }

    @Test
    void mapsResultToResponse() {
        UUID memberId = UUID.randomUUID();
        MemberProfileResult result = result(memberId);

        MemberProfileResponse response = mapper.toResponse(result);

        assertThat(response.memberId()).isEqualTo(memberId.toString());
        assertThat(response.memberNumber()).isEqualTo("MB-1A2B3C4D5E6F7A8B");
        assertThat(response.status()).isEqualTo("INVITED");
        assertThat(response.participations()).singleElement()
                .satisfies(participation -> assertThat(participation.role()).isEqualTo("MEMBER"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("member.read"));
    }

    private MemberProfileResult result(UUID memberId) {
        Instant now = Instant.parse("2026-07-06T08:00:00Z");
        return new MemberProfileResult(
                memberId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "MB-1A2B3C4D5E6F7A8B",
                "INVITED",
                List.of(new GroupParticipationResult(UUID.randomUUID(), "MEMBER", now, null, "INVITED")),
                List.of(),
                0);
    }
}
