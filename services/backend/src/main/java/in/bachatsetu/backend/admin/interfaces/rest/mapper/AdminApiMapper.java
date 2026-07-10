package in.bachatsetu.backend.admin.interfaces.rest.mapper;

import in.bachatsetu.backend.admin.application.command.DisableUserCommand;
import in.bachatsetu.backend.admin.application.command.EnableUserCommand;
import in.bachatsetu.backend.admin.application.query.PlatformGroupResult;
import in.bachatsetu.backend.admin.application.query.PlatformStatisticsResult;
import in.bachatsetu.backend.admin.application.query.PlatformTenantResult;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformPageRequest;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSortField;
import in.bachatsetu.backend.admin.domain.port.SortDirection;
import in.bachatsetu.backend.admin.interfaces.rest.config.AdminProperties;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformGroupResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformStatisticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformTenantResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformUserResponse;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Admin application commands and safe responses. */
@Component
public class AdminApiMapper {

    private final AdminProperties properties;

    public AdminApiMapper(AdminProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public PlatformUserSearchCriteria toUserSearchCriteria(
            String status,
            String email,
            String phone,
            Instant createdAfter,
            Instant createdBefore,
            Integer page,
            Integer size,
            String sort,
            String direction) {
        return new PlatformUserSearchCriteria(
                status == null ? null : PlatformUserStatus.valueOf(status),
                email,
                phone,
                createdAfter,
                createdBefore,
                resolvePage(page),
                resolveSize(size),
                toSortField(sort),
                toSortDirection(direction));
    }

    public PlatformGroupSearchCriteria toGroupSearchCriteria(
            String status,
            Instant createdAfter,
            Instant createdBefore,
            Integer page,
            Integer size,
            String direction) {
        return new PlatformGroupSearchCriteria(
                status == null ? null : PlatformGroupStatus.valueOf(status),
                createdAfter,
                createdBefore,
                resolvePage(page),
                resolveSize(size),
                toSortDirection(direction));
    }

    public PlatformPageRequest toPageRequest(Integer page, Integer size) {
        return new PlatformPageRequest(resolvePage(page), resolveSize(size));
    }

    public EnableUserCommand toEnableCommand(String userId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new EnableUserCommand(toUserId(userId), currentUser.userId().toAggregateId());
    }

    public DisableUserCommand toDisableCommand(String userId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new DisableUserCommand(toUserId(userId), currentUser.userId().toAggregateId());
    }

    public PlatformUserResponse toResponse(PlatformUserResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PlatformUserResponse(
                result.userId().toString(),
                result.tenantId().toString(),
                result.email(),
                result.phoneNumber(),
                result.firstName(),
                result.lastName(),
                result.status().name(),
                result.createdAt());
    }

    public PlatformGroupResponse toResponse(PlatformGroupResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PlatformGroupResponse(
                result.groupId().toString(),
                result.tenantId().toString(),
                result.code(),
                result.name(),
                result.status().name(),
                result.memberCount(),
                result.createdAt());
    }

    public PlatformTenantResponse toResponse(PlatformTenantResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PlatformTenantResponse(result.tenantId().toString(), result.userCount(), result.groupCount());
    }

    public PlatformStatisticsResponse toResponse(PlatformStatisticsResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PlatformStatisticsResponse(
                result.totalUsers(),
                result.activeUsers(),
                result.disabledUsers(),
                result.totalGroups(),
                result.activeGroups(),
                result.totalPayments(),
                result.completedPayments(),
                result.totalReceipts(),
                result.totalNotifications(),
                result.totalFiles());
    }

    public PageResponse<PlatformUserResponse> toUserPageResponse(PlatformPage<PlatformUserResult> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<PlatformUserResponse> content = page.content().stream().map(this::toResponse).toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(), page.hasNext(),
                page.hasPrevious());
    }

    public PageResponse<PlatformGroupResponse> toGroupPageResponse(PlatformPage<PlatformGroupResult> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<PlatformGroupResponse> content = page.content().stream().map(this::toResponse).toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(), page.hasNext(),
                page.hasPrevious());
    }

    public PageResponse<PlatformTenantResponse> toTenantPageResponse(PlatformPage<PlatformTenantResult> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<PlatformTenantResponse> content = page.content().stream().map(this::toResponse).toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(), page.hasNext(),
                page.hasPrevious());
    }

    private AggregateId toUserId(String userId) {
        Objects.requireNonNull(userId, "user id must not be null");
        return AggregateId.from(userId);
    }

    private int resolvePage(Integer page) {
        return page == null ? 0 : page;
    }

    private int resolveSize(Integer size) {
        int resolved = size == null ? properties.pageSizeDefault() : size;
        if (resolved > properties.pageSizeMax()) {
            throw new IllegalArgumentException(
                    "size must not exceed the configured maximum of " + properties.pageSizeMax());
        }
        return resolved;
    }

    private PlatformUserSortField toSortField(String sort) {
        return switch (sort) {
            case "createdAt" -> PlatformUserSortField.CREATED_AT;
            case "firstName" -> PlatformUserSortField.FIRST_NAME;
            case "lastName" -> PlatformUserSortField.LAST_NAME;
            case "email" -> PlatformUserSortField.EMAIL;
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
