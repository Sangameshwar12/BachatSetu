package in.bachatsetu.backend.member.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.member.application.command.CreateMemberProfileCommand;
import in.bachatsetu.backend.member.application.command.JoinGroupParticipationCommand;
import in.bachatsetu.backend.member.application.query.GroupParticipationResult;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.interfaces.rest.dto.CreateMemberProfileRequest;
import in.bachatsetu.backend.member.interfaces.rest.dto.GroupParticipationResponse;
import in.bachatsetu.backend.member.interfaces.rest.dto.JoinGroupParticipationRequest;
import in.bachatsetu.backend.member.interfaces.rest.dto.MemberProfileResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Member application commands and safe responses. */
@Component
public class MemberApiMapper {

    public CreateMemberProfileCommand toCreateCommand(
            CreateMemberProfileRequest request,
            AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new CreateMemberProfileCommand(
                currentUser.tenantId(),
                AggregateId.from(request.userId()),
                AggregateId.from(request.groupId()),
                GroupRole.valueOf(request.role()),
                currentUser.userId().toAggregateId());
    }

    public JoinGroupParticipationCommand toJoinCommand(
            String memberId,
            JoinGroupParticipationRequest request,
            AuthenticatedUser currentUser) {
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new JoinGroupParticipationCommand(
                currentUser.tenantId(),
                AggregateId.from(memberId),
                AggregateId.from(request.groupId()),
                GroupRole.valueOf(request.role()),
                currentUser.userId().toAggregateId());
    }

    public MemberProfileResult getMember(
            GetMemberProfileUseCase useCase,
            AuthenticatedUser currentUser,
            String memberId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        return useCase.execute(currentUser.tenantId(), AggregateId.from(memberId));
    }

    public MemberProfileResponse toResponse(MemberProfileResult result) {
        Objects.requireNonNull(result, "result must not be null");
        List<GroupParticipationResponse> participations = result.participations().stream()
                .map(this::toParticipationResponse)
                .toList();
        return new MemberProfileResponse(
                result.memberId().toString(),
                result.tenantId().toString(),
                result.userId().toString(),
                result.memberNumber(),
                result.status(),
                participations,
                result.version());
    }

    public GroupParticipationResponse toParticipationResponse(GroupParticipationResult participation) {
        Objects.requireNonNull(participation, "participation must not be null");
        return new GroupParticipationResponse(
                participation.groupId().toString(),
                participation.role(),
                participation.joinedAt(),
                participation.exitedAt(),
                participation.status());
    }
}
