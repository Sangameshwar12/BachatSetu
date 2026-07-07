package in.bachatsetu.backend.member.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.member.application.command.CreateMemberProfileCommand;
import in.bachatsetu.backend.member.application.command.JoinGroupParticipationCommand;
import in.bachatsetu.backend.member.application.command.UpdateMemberProfileCommand;
import in.bachatsetu.backend.member.application.query.GroupParticipationResult;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.query.MemberProfileSummary;
import in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase;
import in.bachatsetu.backend.member.application.usecase.ListMemberProfilesUseCase;
import in.bachatsetu.backend.member.domain.port.MemberPage;
import in.bachatsetu.backend.member.domain.port.MemberPageRequest;
import in.bachatsetu.backend.member.domain.port.MemberSortField;
import in.bachatsetu.backend.member.domain.port.SortDirection;
import in.bachatsetu.backend.member.interfaces.rest.dto.CreateMemberProfileRequest;
import in.bachatsetu.backend.member.interfaces.rest.dto.GroupParticipationResponse;
import in.bachatsetu.backend.member.interfaces.rest.dto.JoinGroupParticipationRequest;
import in.bachatsetu.backend.member.interfaces.rest.dto.MemberProfileResponse;
import in.bachatsetu.backend.member.interfaces.rest.dto.MemberProfileSummaryResponse;
import in.bachatsetu.backend.member.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.member.interfaces.rest.dto.UpdateMemberProfileRequest;
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

    @Test
    void getParticipationsDelegatesToUseCaseAndExtractsParticipations() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID memberId = UUID.randomUUID();
        MemberProfileResult expected = result(memberId);
        GetMemberProfileUseCase useCase = (tenantId, id) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(id.value()).isEqualTo(memberId);
            return expected;
        };

        List<GroupParticipationResponse> participations =
                mapper.getParticipations(useCase, currentUser, memberId.toString());

        assertThat(participations).singleElement()
                .satisfies(participation -> assertThat(participation.role()).isEqualTo("MEMBER"));
    }

    @Test
    void buildsPageRequestFromValidatedRestParameters() {
        MemberPageRequest pageRequest = mapper.toPageRequest(1, 10, "memberNumber", "desc");

        assertThat(pageRequest.page()).isEqualTo(1);
        assertThat(pageRequest.size()).isEqualTo(10);
        assertThat(pageRequest.sortField()).isEqualTo(MemberSortField.MEMBER_NUMBER);
        assertThat(pageRequest.direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void rejectsUnsupportedSortOrDirectionValues() {
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "unsupported", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "memberNumber", "sideways"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listMembersDelegatesToUseCaseWithTenantIdentityAndPageRequest() {
        AuthenticatedUser currentUser = authenticatedUser();
        MemberPageRequest pageRequest = new MemberPageRequest(0, 20, MemberSortField.CREATED_AT, SortDirection.ASC);
        MemberPage<MemberProfileSummary> expected = new MemberPage<>(List.of(summary()), 0, 20, 1);
        ListMemberProfilesUseCase useCase = (tenantId, request) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(request).isEqualTo(pageRequest);
            return expected;
        };

        assertThat(mapper.listMembers(useCase, currentUser, pageRequest)).isEqualTo(expected);
    }

    @Test
    void listMembersConsolidatesPageRequestAndResponseForTheController() {
        AuthenticatedUser currentUser = authenticatedUser();
        ListMemberProfilesUseCase useCase = (tenantId, request) -> new MemberPage<>(List.of(summary()), 0, 20, 1);

        PageResponse<MemberProfileSummaryResponse> response =
                mapper.listMembers(useCase, currentUser, 0, 20, "createdAt", "asc");

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void mapsSummaryToResponse() {
        MemberProfileSummaryResponse response = mapper.toSummaryResponse(summary());

        assertThat(response.participationCount()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void mapsMemberPageToPageResponse() {
        MemberPage<MemberProfileSummary> page = new MemberPage<>(List.of(summary(), summary()), 0, 2, 3);

        PageResponse<MemberProfileSummaryResponse> response = mapper.toSummaryPage(page);

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    void mapsUpdateRequestToCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID memberId = UUID.randomUUID();
        UpdateMemberProfileRequest request = new UpdateMemberProfileRequest("ACTIVE");

        UpdateMemberProfileCommand command = mapper.toUpdateCommand(memberId.toString(), request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.memberId().value()).isEqualTo(memberId);
        assertThat(command.status().name()).isEqualTo("ACTIVE");
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    private MemberProfileSummary summary() {
        return new MemberProfileSummary(UUID.randomUUID(), UUID.randomUUID(), "MB-1A2B3C4D5E6F7A8B", "ACTIVE", 2);
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
