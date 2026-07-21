package in.bachatsetu.backend.group.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import in.bachatsetu.backend.group.application.exception.DuplicateGroupCodeException;
import in.bachatsetu.backend.group.application.exception.GroupAccessDeniedException;
import in.bachatsetu.backend.group.application.exception.SavingsGroupNotFoundException;
import in.bachatsetu.backend.group.application.query.GroupMemberResult;
import in.bachatsetu.backend.group.application.port.GroupPage;
import in.bachatsetu.backend.group.application.port.GroupPageRequest;
import in.bachatsetu.backend.group.application.port.GroupSortField;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupSummary;
import in.bachatsetu.backend.group.application.port.SortDirection;
import in.bachatsetu.backend.group.application.usecase.ActivateGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.CloseGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.CreateSavingsGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.GetSavingsGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.JoinGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.ListSavingsGroupsUseCase;
import in.bachatsetu.backend.group.application.usecase.RemoveMemberUseCase;
import in.bachatsetu.backend.group.application.usecase.SuspendGroupUseCase;
import in.bachatsetu.backend.group.domain.exception.DuplicateMemberException;
import in.bachatsetu.backend.group.domain.exception.InvalidGroupStateException;
import in.bachatsetu.backend.group.domain.exception.OwnerRemovalNotAllowedException;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.group.interfaces.rest.exception.SavingsGroupExceptionHandler;
import in.bachatsetu.backend.group.interfaces.rest.mapper.SavingsGroupApiMapper;
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

@WebMvcTest(SavingsGroupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SavingsGroupApiMapper.class, SavingsGroupExceptionHandler.class})
class SavingsGroupControllerTest {

    private static final AggregateId TENANT_ID = AggregateId.newId();
    private static final AggregateId OWNER_ID = AggregateId.newId();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateSavingsGroupUseCase createSavingsGroup;

    @MockBean
    private GetSavingsGroupUseCase getSavingsGroup;

    @MockBean
    private ListSavingsGroupsUseCase listSavingsGroups;

    @MockBean
    private ActivateGroupUseCase activateGroup;

    @MockBean
    private SuspendGroupUseCase suspendGroup;

    @MockBean
    private CloseGroupUseCase closeGroup;

    @MockBean
    private JoinGroupUseCase joinGroup;

    @MockBean
    private RemoveMemberUseCase removeMember;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsSavingsGroup() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID groupId = UUID.randomUUID();
        when(createSavingsGroup.execute(any())).thenReturn(result(groupId, "INACTIVE"));

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/groups/" + groupId))
                .andExpect(jsonPath("$.groupId").value(groupId.toString()))
                .andExpect(jsonPath("$.groupCode").value("BS-1A2B3C4D5E6F7A8B"))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.contributionAmountPaise").value(500000));
    }

    @Test
    void rejectsBlankGroupName() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyWithName("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void reportsDuplicateGroupCodeAsConflict() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(createSavingsGroup.execute(any()))
                .thenThrow(new DuplicateGroupCodeException("generated group code already exists"));

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("group-code-conflict"));
    }

    @Test
    void rejectsUnauthenticatedCreateRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void getsSavingsGroup() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID groupId = UUID.randomUUID();
        when(getSavingsGroup.execute(eq(TENANT_ID), any())).thenReturn(result(groupId, "ACTIVE"));

        mockMvc.perform(get("/api/v1/groups/" + groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void reportsMissingGroupAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID groupId = UUID.randomUUID();
        when(getSavingsGroup.execute(eq(TENANT_ID), any()))
                .thenThrow(new SavingsGroupNotFoundException("savings group does not exist"));

        mockMvc.perform(get("/api/v1/groups/" + groupId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("group-not-found"));
    }

    @Test
    void rejectsUnauthenticatedGetRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/groups/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void listsSavingsGroupsWithDefaultPagination() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        GroupPageRequest expectedRequest = new GroupPageRequest(
                0, 20, GroupSortField.CREATED_AT, SortDirection.ASC, null);
        when(listSavingsGroups.execute(TENANT_ID, expectedRequest))
                .thenReturn(new GroupPage<>(List.of(summary(), summary()), 0, 20, 2));

        mockMvc.perform(get("/api/v1/groups"))
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
    void listsSavingsGroupsWithExplicitPagination() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        GroupPageRequest expectedRequest = new GroupPageRequest(
                0, 2, GroupSortField.CREATED_AT, SortDirection.ASC, null);
        when(listSavingsGroups.execute(TENANT_ID, expectedRequest))
                .thenReturn(new GroupPage<>(List.of(summary(), summary()), 0, 2, 3));

        mockMvc.perform(get("/api/v1/groups").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void listsSavingsGroupsWithSortDirectionAndStatusFilter() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        GroupPageRequest expectedRequest = new GroupPageRequest(
                0, 20, GroupSortField.NAME, SortDirection.DESC, GroupStatus.ACTIVE);
        when(listSavingsGroups.execute(TENANT_ID, expectedRequest))
                .thenReturn(new GroupPage<>(List.of(summary()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/groups")
                        .param("sort", "name")
                        .param("direction", "desc")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void rejectsInvalidSortAndStatusParameters() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(get("/api/v1/groups").param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/groups").param("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/groups").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void rejectsInvalidPaginationParameters() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(get("/api/v1/groups").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/groups").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void activatesSavingsGroup() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID groupId = UUID.randomUUID();
        when(activateGroup.execute(any())).thenReturn(result(groupId, "ACTIVE"));

        mockMvc.perform(patch("/api/v1/groups/" + groupId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void reportsInvalidActivationTransitionAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(activateGroup.execute(any()))
                .thenThrow(new InvalidGroupStateException("group cannot transition from CLOSED to ACTIVE"));

        mockMvc.perform(patch("/api/v1/groups/" + UUID.randomUUID() + "/activate"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("group-validation-failed"));
    }

    @Test
    void suspendsSavingsGroup() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID groupId = UUID.randomUUID();
        when(suspendGroup.execute(any())).thenReturn(result(groupId, "SUSPENDED"));

        mockMvc.perform(patch("/api/v1/groups/" + groupId + "/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void closesSavingsGroup() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID groupId = UUID.randomUUID();
        when(closeGroup.execute(any())).thenReturn(result(groupId, "CLOSED"));

        mockMvc.perform(patch("/api/v1/groups/" + groupId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void rejectsUnauthenticatedLifecycleRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(patch("/api/v1/groups/" + UUID.randomUUID() + "/activate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void reportsNonOwnerActivationAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(activateGroup.execute(any()))
                .thenThrow(new GroupAccessDeniedException("only the group owner may perform this operation"));

        mockMvc.perform(patch("/api/v1/groups/" + UUID.randomUUID() + "/activate"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void reportsNonOwnerSuspensionAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(suspendGroup.execute(any()))
                .thenThrow(new GroupAccessDeniedException("only the group owner may perform this operation"));

        mockMvc.perform(patch("/api/v1/groups/" + UUID.randomUUID() + "/suspend"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void reportsNonOwnerClosureAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(closeGroup.execute(any()))
                .thenThrow(new GroupAccessDeniedException("only the group owner may perform this operation"));

        mockMvc.perform(patch("/api/v1/groups/" + UUID.randomUUID() + "/close"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void reportsCrossTenantActivationAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(activateGroup.execute(any()))
                .thenThrow(new SavingsGroupNotFoundException("savings group does not exist"));

        mockMvc.perform(patch("/api/v1/groups/" + UUID.randomUUID() + "/activate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("group-not-found"));
    }

    @Test
    void addsGroupMember() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID groupId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-06T08:00:00Z");
        SavingsGroupResult groupWithMember = new SavingsGroupResult(
                groupId, TENANT_ID.value(), OWNER_ID.value(), "BS-1A2B3C4D5E6F7A8B", "Sunrise Bhishi Circle",
                "Monthly society savings", "BHISHI", "ACTIVE", 500_000L, "INR", 10, 2, now, now, 1,
                List.of(
                        new GroupMemberResult(OWNER_ID.value(), now, null, true),
                        new GroupMemberResult(memberId, now, null, true)),
                "Priya Sharma");
        when(joinGroup.execute(any())).thenReturn(groupWithMember);

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"" + memberId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/groups/" + groupId + "/members/" + memberId))
                .andExpect(jsonPath("$.memberId").value(memberId.toString()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void rejectsInvalidMemberIdFormat() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/groups/" + UUID.randomUUID() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"not-a-uuid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void reportsDuplicateMemberAsConflict() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(joinGroup.execute(any())).thenThrow(new DuplicateMemberException("member has already joined the group"));

        mockMvc.perform(post("/api/v1/groups/" + UUID.randomUUID() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member-already-joined"));
    }

    @Test
    void removesGroupMember() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID groupId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/groups/" + groupId + "/members/" + memberId))
                .andExpect(status().isNoContent());

        verify(removeMember).execute(any());
    }

    @Test
    void reportsOwnerRemovalAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        doThrow(new OwnerRemovalNotAllowedException("the group owner cannot be removed"))
                .when(removeMember).execute(any());

        mockMvc.perform(delete("/api/v1/groups/" + UUID.randomUUID() + "/members/" + UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("group-validation-failed"));
    }

    @Test
    void reportsNonOwnerAddMemberAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(joinGroup.execute(any()))
                .thenThrow(new GroupAccessDeniedException("only the group owner may perform this operation"));

        mockMvc.perform(post("/api/v1/groups/" + UUID.randomUUID() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void reportsNonOwnerRemoveMemberAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        doThrow(new GroupAccessDeniedException("only the group owner may perform this operation"))
                .when(removeMember).execute(any());

        mockMvc.perform(delete("/api/v1/groups/" + UUID.randomUUID() + "/members/" + UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void rejectsUnauthenticatedMembershipRequests() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/groups/" + UUID.randomUUID() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));

        mockMvc.perform(delete("/api/v1/groups/" + UUID.randomUUID() + "/members/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.from(OWNER_ID.toString()),
                MobileNumber.of("+919876543210"),
                TENANT_ID,
                Set.of("GROUP_MEMBER"),
                Set.of("group.read"));
    }

    private SavingsGroupResult result(UUID groupId, String status) {
        Instant now = Instant.parse("2026-07-06T08:00:00Z");
        return new SavingsGroupResult(
                groupId,
                TENANT_ID.value(),
                OWNER_ID.value(),
                "BS-1A2B3C4D5E6F7A8B",
                "Sunrise Bhishi Circle",
                "Monthly society savings",
                "BHISHI",
                status,
                500_000L,
                "INR",
                10,
                1,
                now,
                now,
                0,
                List.of(new GroupMemberResult(OWNER_ID.value(), now, null, true)),
                "Priya Sharma");
    }

    private SavingsGroupSummary summary() {
        return new SavingsGroupSummary(
                UUID.randomUUID(), "BS-1A2B3C4D5E6F7A8B", "Sunrise Bhishi Circle", "ACTIVE", 500_000L, "INR", 10, 1);
    }

    private String validRequestBody() {
        return requestBodyWithName("Sunrise Bhishi Circle");
    }

    private String requestBodyWithName(String name) {
        return """
                {
                  "name": "%s",
                  "description": "Monthly society savings",
                  "type": "BHISHI",
                  "rule": {
                    "contributionSchedule": {
                      "contributionAmountPaise": 500000,
                      "frequency": "MONTHLY",
                      "startDate": "2026-08-01",
                      "cycleCount": 12
                    },
                    "memberCapacity": {
                      "minimum": 2,
                      "maximum": 10
                    },
                    "payoutMethod": "RANDOM_DRAW",
                    "partialPaymentsAllowed": false
                  }
                }
                """.formatted(name);
    }
}
