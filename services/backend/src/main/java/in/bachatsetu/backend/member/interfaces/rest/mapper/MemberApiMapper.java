package in.bachatsetu.backend.member.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.member.application.command.CreateMemberProfileCommand;
import in.bachatsetu.backend.member.application.command.JoinGroupParticipationCommand;
import in.bachatsetu.backend.member.application.command.UpdateMemberProfileCommand;
import in.bachatsetu.backend.member.application.query.GroupParticipationResult;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.query.MemberProfileSummary;
import in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase;
import in.bachatsetu.backend.member.application.usecase.ListMemberProfilesUseCase;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.MemberStatus;
import in.bachatsetu.backend.member.domain.port.MemberPage;
import in.bachatsetu.backend.member.domain.port.MemberPageRequest;
import in.bachatsetu.backend.member.domain.port.MemberSortField;
import in.bachatsetu.backend.member.domain.port.SortDirection;
import in.bachatsetu.backend.member.interfaces.rest.dto.CreateMemberProfileRequest;
import in.bachatsetu.backend.member.interfaces.rest.dto.GroupParticipationResponse;
import in.bachatsetu.backend.member.interfaces.rest.dto.JoinGroupParticipationRequest;
import in.bachatsetu.backend.member.interfaces.rest.dto.MemberProfileResponse;
import in.bachatsetu.backend.member.interfaces.rest.dto.MemberProfileSummaryResponse;
import in.bachatsetu.backend.member.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.member.interfaces.rest.dto.UpdateMemberProfileRequest;
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

    public List<GroupParticipationResponse> getParticipations(
            GetMemberProfileUseCase useCase,
            AuthenticatedUser currentUser,
            String memberId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        MemberProfileResult result = useCase.execute(currentUser.tenantId(), AggregateId.from(memberId));
        return result.participations().stream().map(this::toParticipationResponse).toList();
    }

    public MemberPageRequest toPageRequest(int page, int size, String sort, String direction) {
        return new MemberPageRequest(page, size, toSortField(sort), toSortDirection(direction));
    }

    public MemberPage<MemberProfileSummary> listMembers(
            ListMemberProfilesUseCase useCase,
            AuthenticatedUser currentUser,
            MemberPageRequest pageRequest) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return useCase.execute(currentUser.tenantId(), pageRequest);
    }

    public PageResponse<MemberProfileSummaryResponse> listMembers(
            ListMemberProfilesUseCase useCase,
            AuthenticatedUser currentUser,
            int page,
            int size,
            String sort,
            String direction) {
        MemberPageRequest pageRequest = toPageRequest(page, size, sort, direction);
        return toSummaryPage(listMembers(useCase, currentUser, pageRequest));
    }

    public MemberProfileSummaryResponse toSummaryResponse(MemberProfileSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        return new MemberProfileSummaryResponse(
                summary.memberId().toString(),
                summary.userId().toString(),
                summary.memberNumber(),
                summary.status(),
                summary.participationCount());
    }

    public PageResponse<MemberProfileSummaryResponse> toSummaryPage(MemberPage<MemberProfileSummary> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<MemberProfileSummaryResponse> content = page.content().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(),
                page.hasNext(), page.hasPrevious());
    }

    public UpdateMemberProfileCommand toUpdateCommand(
            String memberId,
            UpdateMemberProfileRequest request,
            AuthenticatedUser currentUser) {
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new UpdateMemberProfileCommand(
                currentUser.tenantId(),
                AggregateId.from(memberId),
                MemberStatus.valueOf(request.status()),
                currentUser.userId().toAggregateId());
    }

    private MemberSortField toSortField(String sort) {
        return switch (sort) {
            case "memberNumber" -> MemberSortField.MEMBER_NUMBER;
            case "createdAt" -> MemberSortField.CREATED_AT;
            default -> throw new IllegalArgumentException("unsupported sort field: " + sort);
        };
    }

    private SortDirection toSortDirection(String direction) {
        return switch (direction) {
            case "asc" -> SortDirection.ASC;
            case "desc" -> SortDirection.DESC;
            default -> throw new IllegalArgumentException("unsupported sort direction: " + direction);
        };
    }
}
