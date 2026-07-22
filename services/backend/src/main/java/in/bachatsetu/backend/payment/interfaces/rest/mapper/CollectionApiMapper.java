package in.bachatsetu.backend.payment.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.payment.application.command.RecordManualPaymentCommand;
import in.bachatsetu.backend.payment.application.query.CollectionSummaryResult;
import in.bachatsetu.backend.payment.application.query.MemberCollectionResult;
import in.bachatsetu.backend.payment.application.usecase.GetCollectionSummaryUseCase;
import in.bachatsetu.backend.payment.interfaces.rest.dto.CollectionSummaryResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.MemberCollectionResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Collection application commands and safe responses. */
@Component
public class CollectionApiMapper {

    public CollectionSummaryResult getSummary(
            GetCollectionSummaryUseCase useCase, AuthenticatedUser currentUser, String groupId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        return useCase.execute(currentUser.tenantId(), GroupId.from(groupId));
    }

    public CollectionSummaryResponse toResponse(CollectionSummaryResult result) {
        Objects.requireNonNull(result, "result must not be null");
        List<MemberCollectionResponse> members = result.members().stream().map(this::toMemberResponse).toList();
        return new CollectionSummaryResponse(
                result.groupId().toString(),
                result.cycleActive(),
                result.cycleNumber(),
                result.cycleStart(),
                result.cycleEnd(),
                result.dueDate(),
                result.contributionAmountPaise(),
                result.currencyCode(),
                result.totalMembers(),
                result.paidCount(),
                result.pendingCount(),
                result.overdueCount(),
                result.totalExpectedPaise(),
                result.totalCollectedPaise(),
                result.totalRemainingPaise(),
                members);
    }

    public MemberCollectionResponse toMemberResponse(MemberCollectionResult member) {
        Objects.requireNonNull(member, "member must not be null");
        return new MemberCollectionResponse(
                member.memberId().toString(),
                member.memberName(),
                member.status(),
                member.expectedAmountPaise(),
                member.collectedAmountPaise(),
                member.paidAt(),
                member.dueDate());
    }

    public RecordManualPaymentCommand toMarkPaidCommand(
            String groupId, String memberId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new RecordManualPaymentCommand(
                currentUser.tenantId(),
                AggregateId.from(groupId),
                AggregateId.from(memberId),
                currentUser.userId().toAggregateId());
    }
}
