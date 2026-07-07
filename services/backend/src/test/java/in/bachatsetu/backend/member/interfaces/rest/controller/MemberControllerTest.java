package in.bachatsetu.backend.member.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.member.application.exception.DuplicateMemberNumberException;
import in.bachatsetu.backend.member.application.exception.MemberAccessDeniedException;
import in.bachatsetu.backend.member.application.exception.MemberProfileNotFoundException;
import in.bachatsetu.backend.member.application.query.GroupParticipationResult;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.query.MemberProfileSummary;
import in.bachatsetu.backend.member.application.usecase.CreateMemberProfileUseCase;
import in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase;
import in.bachatsetu.backend.member.application.usecase.JoinGroupParticipationUseCase;
import in.bachatsetu.backend.member.application.usecase.ListMemberProfilesUseCase;
import in.bachatsetu.backend.member.application.usecase.UpdateMemberProfileUseCase;
import in.bachatsetu.backend.member.domain.exception.DuplicateGroupParticipationException;
import in.bachatsetu.backend.member.domain.exception.InvalidMembershipStateException;
import in.bachatsetu.backend.member.domain.port.MemberPage;
import in.bachatsetu.backend.member.domain.port.MemberPageRequest;
import in.bachatsetu.backend.member.domain.port.MemberSortField;
import in.bachatsetu.backend.member.domain.port.SortDirection;
import in.bachatsetu.backend.member.interfaces.rest.exception.MemberExceptionHandler;
import in.bachatsetu.backend.member.interfaces.rest.mapper.MemberApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({MemberApiMapper.class, MemberExceptionHandler.class})
class MemberControllerTest {

    private static final AggregateId TENANT_ID = AggregateId.newId();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateMemberProfileUseCase createMemberProfile;

    @MockBean
    private JoinGroupParticipationUseCase joinGroupParticipation;

    @MockBean
    private GetMemberProfileUseCase getMemberProfile;

    @MockBean
    private ListMemberProfilesUseCase listMemberProfiles;

    @MockBean
    private UpdateMemberProfileUseCase updateMemberProfile;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsMemberProfile() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID memberId = UUID.randomUUID();
        when(createMemberProfile.execute(any())).thenReturn(result(memberId, "INVITED"));

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/members/" + memberId))
                .andExpect(jsonPath("$.memberId").value(memberId.toString()))
                .andExpect(jsonPath("$.status").value("INVITED"))
                .andExpect(jsonPath("$.memberNumber").value("MB-1A2B3C4D5E6F7A8B"));
    }

    @Test
    void rejectsInvalidUserIdFormat() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "not-a-uuid", "groupId": "%s", "role": "MEMBER"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void reportsDuplicateMemberNumberAsConflict() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(createMemberProfile.execute(any()))
                .thenThrow(new DuplicateMemberNumberException("generated member number already exists"));

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member-number-conflict"));
    }

    @Test
    void rejectsUnauthenticatedCreateRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void getsMemberProfile() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID memberId = UUID.randomUUID();
        when(getMemberProfile.execute(eq(TENANT_ID), any(), any())).thenReturn(result(memberId, "ACTIVE"));

        mockMvc.perform(get("/api/v1/members/" + memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(memberId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void reportsMissingMemberAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getMemberProfile.execute(eq(TENANT_ID), any(), any()))
                .thenThrow(new MemberProfileNotFoundException("member profile does not exist"));

        mockMvc.perform(get("/api/v1/members/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("member-not-found"));
    }

    @Test
    void rejectsUnauthenticatedGetRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/members/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void reportsViewingAnotherMembersProfileAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getMemberProfile.execute(eq(TENANT_ID), any(), any()))
                .thenThrow(new MemberAccessDeniedException("only the member themselves may perform this operation"));

        mockMvc.perform(get("/api/v1/members/" + UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void listsMemberProfilesWithDefaultPagination() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        MemberPageRequest expectedRequest = new MemberPageRequest(0, 20, MemberSortField.CREATED_AT, SortDirection.ASC);
        when(listMemberProfiles.execute(TENANT_ID, expectedRequest))
                .thenReturn(new MemberPage<>(List.of(summary(), summary()), 0, 20, 2));

        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void listsMemberProfilesWithExplicitPaginationAndSort() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        MemberPageRequest expectedRequest = new MemberPageRequest(1, 2, MemberSortField.MEMBER_NUMBER, SortDirection.DESC);
        when(listMemberProfiles.execute(TENANT_ID, expectedRequest))
                .thenReturn(new MemberPage<>(List.of(summary()), 1, 2, 3));

        mockMvc.perform(get("/api/v1/members")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "memberNumber")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.hasPrevious").value(true))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void rejectsInvalidSortAndPaginationParameters() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(get("/api/v1/members").param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/members").param("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/members").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/members").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void rejectsUnauthenticatedListRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void getsAMembersParticipations() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID memberId = UUID.randomUUID();
        when(getMemberProfile.execute(eq(TENANT_ID), any(), any())).thenReturn(result(memberId, "ACTIVE"));

        mockMvc.perform(get("/api/v1/members/" + memberId + "/participations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("MEMBER"));
    }

    @Test
    void reportsMissingMemberOnParticipationsAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getMemberProfile.execute(eq(TENANT_ID), any(), any()))
                .thenThrow(new MemberProfileNotFoundException("member profile does not exist"));

        mockMvc.perform(get("/api/v1/members/" + UUID.randomUUID() + "/participations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("member-not-found"));
    }

    @Test
    void reportsViewingAnotherMembersParticipationsAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getMemberProfile.execute(eq(TENANT_ID), any(), any()))
                .thenThrow(new MemberAccessDeniedException("only the member themselves may perform this operation"));

        mockMvc.perform(get("/api/v1/members/" + UUID.randomUUID() + "/participations"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void rejectsUnauthenticatedParticipationsRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/members/" + UUID.randomUUID() + "/participations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void joinsAnAdditionalGroup() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID memberId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        when(joinGroupParticipation.execute(any())).thenReturn(result(memberId, "ACTIVE"));

        mockMvc.perform(post("/api/v1/members/" + memberId + "/participations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": "%s", "role": "MEMBER"}
                                """.formatted(groupId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/members/" + memberId + "/participations/" + groupId))
                .andExpect(jsonPath("$.memberId").value(memberId.toString()));
    }

    @Test
    void reportsDuplicateParticipationAsConflict() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(joinGroupParticipation.execute(any()))
                .thenThrow(new DuplicateGroupParticipationException("member already participates in the group"));

        mockMvc.perform(post("/api/v1/members/" + UUID.randomUUID() + "/participations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": "%s", "role": "MEMBER"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("participation-already-exists"));
    }

    @Test
    void rejectsUnauthenticatedJoinRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/members/" + UUID.randomUUID() + "/participations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": "%s", "role": "MEMBER"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void updatesMemberProfileStatus() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID memberId = UUID.randomUUID();
        when(updateMemberProfile.execute(any())).thenReturn(result(memberId, "ACTIVE"));

        mockMvc.perform(patch("/api/v1/members/" + memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectsInvalidStatusValue() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(patch("/api/v1/members/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "BOGUS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void reportsInvalidStatusTransitionAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(updateMemberProfile.execute(any()))
                .thenThrow(new InvalidMembershipStateException("member cannot transition from ACTIVE to ACTIVE"));

        mockMvc.perform(patch("/api/v1/members/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("member-validation-failed"));
    }

    @Test
    void reportsMissingMemberOnUpdateAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(updateMemberProfile.execute(any()))
                .thenThrow(new MemberProfileNotFoundException("member profile does not exist"));

        mockMvc.perform(patch("/api/v1/members/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("member-not-found"));
    }

    @Test
    void reportsUpdatingAnotherMembersStatusAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(updateMemberProfile.execute(any()))
                .thenThrow(new MemberAccessDeniedException("only the member themselves may perform this operation"));

        mockMvc.perform(patch("/api/v1/members/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void rejectsUnauthenticatedUpdateRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(patch("/api/v1/members/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    private MemberProfileSummary summary() {
        return new MemberProfileSummary(UUID.randomUUID(), UUID.randomUUID(), "MB-1A2B3C4D5E6F7A8B", "ACTIVE", 1);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                TENANT_ID,
                Set.of("GROUP_MEMBER"),
                Set.of("member.read"));
    }

    private MemberProfileResult result(UUID memberId, String status) {
        Instant now = Instant.parse("2026-07-06T08:00:00Z");
        return new MemberProfileResult(
                memberId,
                TENANT_ID.value(),
                UUID.randomUUID(),
                "MB-1A2B3C4D5E6F7A8B",
                status,
                List.of(new GroupParticipationResult(UUID.randomUUID(), "MEMBER", now, null, "ACTIVE")),
                List.of(),
                0);
    }

    private String validRequestBody() {
        return """
                {
                  "userId": "%s",
                  "groupId": "%s",
                  "role": "MEMBER"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
