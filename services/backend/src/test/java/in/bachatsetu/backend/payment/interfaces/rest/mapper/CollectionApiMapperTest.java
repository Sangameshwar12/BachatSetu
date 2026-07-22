package in.bachatsetu.backend.payment.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.payment.application.command.RecordManualPaymentCommand;
import in.bachatsetu.backend.payment.application.query.CollectionSummaryResult;
import in.bachatsetu.backend.payment.application.query.MemberCollectionResult;
import in.bachatsetu.backend.payment.application.usecase.GetCollectionSummaryUseCase;
import in.bachatsetu.backend.payment.interfaces.rest.dto.CollectionSummaryResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CollectionApiMapperTest {

    private final CollectionApiMapper mapper = new CollectionApiMapper();

    @Test
    void getSummaryDelegatesToUseCaseWithParsedIdentifiers() {
        AuthenticatedUser currentUser = authenticatedUser();
        GroupId groupId = GroupId.newId();
        CollectionSummaryResult expected = summary(groupId.value().value());
        GetCollectionSummaryUseCase useCase = (tenantId, id) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(id).isEqualTo(groupId);
            return expected;
        };

        assertThat(mapper.getSummary(useCase, currentUser, groupId.value().toString())).isEqualTo(expected);
    }

    @Test
    void mapsSummaryResultToResponse() {
        UUID groupId = UUID.randomUUID();
        CollectionSummaryResult result = summary(groupId);

        CollectionSummaryResponse response = mapper.toResponse(result);

        assertThat(response.groupId()).isEqualTo(groupId.toString());
        assertThat(response.cycleActive()).isTrue();
        assertThat(response.cycleNumber()).isEqualTo(1);
        assertThat(response.members()).singleElement()
                .satisfies(member -> {
                    assertThat(member.status()).isEqualTo("PAID");
                    assertThat(member.memberName()).isEqualTo("QA Tester");
                });
    }

    @Test
    void mapsMarkPaidCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID groupId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        RecordManualPaymentCommand command =
                mapper.toMarkPaidCommand(groupId.toString(), memberId.toString(), currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.groupId().value()).isEqualTo(groupId);
        assertThat(command.memberId().value()).isEqualTo(memberId);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("payment.read"));
    }

    private CollectionSummaryResult summary(UUID groupId) {
        LocalDate today = LocalDate.of(2026, 8, 1);
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        return new CollectionSummaryResult(
                groupId, true, 1, today, today.plusMonths(1), today, 100_000, "INR", 1, 1, 0, 0,
                100_000, 100_000, 0,
                List.of(new MemberCollectionResult(
                        UUID.randomUUID(), "QA Tester", "PAID", 100_000, 100_000, now, today)));
    }
}
