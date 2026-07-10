package in.bachatsetu.backend.platformoperations.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.platformoperations.application.query.BroadcastResult;
import in.bachatsetu.backend.platformoperations.application.usecase.SendBroadcastNotificationUseCase;
import in.bachatsetu.backend.platformoperations.interfaces.rest.exception.PlatformOperationsExceptionHandler;
import in.bachatsetu.backend.platformoperations.interfaces.rest.mapper.PlatformOperationsApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BroadcastController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PlatformOperationsApiMapper.class, PlatformOperationsExceptionHandler.class})
class BroadcastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SendBroadcastNotificationUseCase sendBroadcastNotification;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void sendsABroadcastNotification() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(sendBroadcastNotification.execute(any())).thenReturn(new BroadcastResult(10, 9, 1));

        mockMvc.perform(post("/api/v1/platform-operations/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":"ALL_USERS","title":"Notice","message":"Please read this"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientCount").value(10))
                .andExpect(jsonPath("$.sentCount").value(9))
                .andExpect(jsonPath("$.failedCount").value(1));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
