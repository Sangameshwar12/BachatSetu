package in.bachatsetu.backend.user.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.application.exception.OnboardingApplicationException;
import in.bachatsetu.backend.user.application.exception.OnboardingFailureReason;
import in.bachatsetu.backend.user.application.query.OnboardingCompletedResult;
import in.bachatsetu.backend.user.application.usecase.CompleteOnboardingUseCase;
import in.bachatsetu.backend.user.interfaces.rest.exception.UserExceptionHandler;
import in.bachatsetu.backend.user.interfaces.rest.mapper.OnboardingApiMapper;
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

@WebMvcTest(OnboardingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({OnboardingApiMapper.class, UserExceptionHandler.class})
class OnboardingControllerTest {

    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompleteOnboardingUseCase completeOnboarding;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void completesOnboardingForTheCurrentUser() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(completeOnboarding.execute(any())).thenReturn(new OnboardingCompletedResult(
                new AggregateId(USER_ID), "Pune", "Maharashtra", null, true));

        mockMvc.perform(post("/api/v1/users/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city":"Pune","state":"Maharashtra","notificationsEnabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.city").value("Pune"));
    }

    @Test
    void mapsAlreadyOnboardedToConflict() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(completeOnboarding.execute(any())).thenThrow(new OnboardingApplicationException(
                OnboardingFailureReason.ALREADY_ONBOARDED, "already done"));

        mockMvc.perform(post("/api/v1/users/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already-onboarded"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                new UserId(USER_ID), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
