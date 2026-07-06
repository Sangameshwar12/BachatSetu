package in.bachatsetu.backend.group.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.group.application.command.CreateSavingsGroupCommand;
import in.bachatsetu.backend.group.application.query.GroupMemberResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.interfaces.rest.dto.ContributionScheduleRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.CreateSavingsGroupRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.GroupRuleRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.MemberCapacityRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.SavingsGroupResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavingsGroupApiMapperTest {

    private final SavingsGroupApiMapper mapper = new SavingsGroupApiMapper();

    @Test
    void mapsRequestToCommandUsingAuthenticatedIdentityForTenantAndOwner() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("group.read"));
        CreateSavingsGroupRequest request = new CreateSavingsGroupRequest(
                "Sunrise Bhishi Circle",
                "Monthly society savings",
                "BHISHI",
                new GroupRuleRequest(
                        new ContributionScheduleRequest(500_000L, "MONTHLY", LocalDate.of(2026, 8, 1), 12),
                        new MemberCapacityRequest(2, 10),
                        "RANDOM_DRAW",
                        false));

        CreateSavingsGroupCommand command = mapper.toCommand(request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.ownerId().value()).isEqualTo(currentUser.userId().toAggregateId());
        assertThat(command.name().value()).isEqualTo("Sunrise Bhishi Circle");
        assertThat(command.description().value()).isEqualTo("Monthly society savings");
        assertThat(command.type().name()).isEqualTo("BHISHI");
        assertThat(command.rule().contributionSchedule().contributionAmount().minorUnits()).isEqualTo(500_000L);
        assertThat(command.rule().memberCapacity().minimum()).isEqualTo(2);
        assertThat(command.rule().memberCapacity().maximum()).isEqualTo(10);
        assertThat(command.rule().payoutMethod().name()).isEqualTo("RANDOM_DRAW");
    }

    @Test
    void defaultsMissingDescriptionToEmpty() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of(),
                Set.of());
        CreateSavingsGroupRequest request = new CreateSavingsGroupRequest(
                "Sunrise Bhishi Circle",
                null,
                "BHISHI",
                new GroupRuleRequest(
                        new ContributionScheduleRequest(500_000L, "MONTHLY", LocalDate.of(2026, 8, 1), 12),
                        new MemberCapacityRequest(2, 10),
                        "RANDOM_DRAW",
                        false));

        CreateSavingsGroupCommand command = mapper.toCommand(request, currentUser);

        assertThat(command.description().value()).isEmpty();
    }

    @Test
    void mapsResultToResponse() {
        SavingsGroupResult result = new SavingsGroupResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BS-1A2B3C4D5E6F7A8B",
                "Sunrise Bhishi Circle",
                "Monthly society savings",
                "BHISHI",
                "INACTIVE",
                500_000L,
                "INR",
                10,
                1,
                Instant.parse("2026-07-06T08:00:00Z"),
                Instant.parse("2026-07-06T08:00:00Z"),
                0,
                List.of(new GroupMemberResult(UUID.randomUUID(), Instant.parse("2026-07-06T08:00:00Z"), null, true)));

        SavingsGroupResponse response = mapper.toResponse(result);

        assertThat(response.groupId()).isEqualTo(result.groupId().toString());
        assertThat(response.groupCode()).isEqualTo("BS-1A2B3C4D5E6F7A8B");
        assertThat(response.contributionAmountPaise()).isEqualTo(500_000L);
        assertThat(response.currencyCode()).isEqualTo("INR");
        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(response.version()).isZero();
    }
}
