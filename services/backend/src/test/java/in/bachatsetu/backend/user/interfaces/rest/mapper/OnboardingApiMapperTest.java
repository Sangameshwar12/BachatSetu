package in.bachatsetu.backend.user.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.application.command.CompleteOnboardingCommand;
import in.bachatsetu.backend.user.application.query.OnboardingCompletedResult;
import in.bachatsetu.backend.user.interfaces.rest.dto.CompleteOnboardingRequest;
import in.bachatsetu.backend.user.interfaces.rest.dto.OnboardingCompletedResponse;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OnboardingApiMapperTest {

    private final OnboardingApiMapper mapper = new OnboardingApiMapper();

    @Test
    void mapsRequestToCommandWithAPhotoFileId() {
        AuthenticatedUser currentUser = authenticatedUser();
        AggregateId photoFileId = AggregateId.newId();
        CompleteOnboardingRequest request =
                new CompleteOnboardingRequest("Pune", "Maharashtra", photoFileId.value().toString(), true);

        CompleteOnboardingCommand command = mapper.toCommand(currentUser, request);

        assertThat(command.userId()).isEqualTo(currentUser.userId().toAggregateId());
        assertThat(command.city()).isEqualTo("Pune");
        assertThat(command.state()).isEqualTo("Maharashtra");
        assertThat(command.photoFileId()).isEqualTo(photoFileId);
        assertThat(command.notificationsEnabled()).isTrue();
    }

    @Test
    void mapsRequestToCommandWithoutAPhoto() {
        CompleteOnboardingRequest request = new CompleteOnboardingRequest("", "", "", false);

        CompleteOnboardingCommand command = mapper.toCommand(authenticatedUser(), request);

        assertThat(command.city()).isNull();
        assertThat(command.state()).isNull();
        assertThat(command.photoFileId()).isNull();
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }

    @Test
    void mapsResultToResponse() {
        AggregateId userId = AggregateId.newId();
        AggregateId photoFileId = AggregateId.newId();
        OnboardingCompletedResult result =
                new OnboardingCompletedResult(userId, "Pune", "Maharashtra", photoFileId, true);

        OnboardingCompletedResponse response = mapper.toResponse(result);

        assertThat(response.userId()).isEqualTo(userId.toString());
        assertThat(response.city()).isEqualTo("Pune");
        assertThat(response.photoFileId()).isEqualTo(photoFileId.toString());
        assertThat(response.notificationsEnabled()).isTrue();
    }
}
