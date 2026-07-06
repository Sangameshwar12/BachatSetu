package in.bachatsetu.backend.group.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import in.bachatsetu.backend.group.application.query.GroupMemberResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.usecase.CreateSavingsGroupUseCase;
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
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsSavingsGroup() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID groupId = UUID.randomUUID();
        when(createSavingsGroup.execute(any())).thenReturn(result(groupId));

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
    void rejectsUnauthenticatedRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
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

    private SavingsGroupResult result(UUID groupId) {
        Instant now = Instant.parse("2026-07-06T08:00:00Z");
        return new SavingsGroupResult(
                groupId,
                TENANT_ID.value(),
                OWNER_ID.value(),
                "BS-1A2B3C4D5E6F7A8B",
                "Sunrise Bhishi Circle",
                "Monthly society savings",
                "BHISHI",
                "INACTIVE",
                500_000L,
                "INR",
                10,
                1,
                now,
                now,
                0,
                List.of(new GroupMemberResult(OWNER_ID.value(), now, null, true)));
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
